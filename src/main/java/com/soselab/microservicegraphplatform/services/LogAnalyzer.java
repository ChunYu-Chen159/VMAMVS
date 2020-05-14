package com.soselab.microservicegraphplatform.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soselab.microservicegraphplatform.bean.elasticsearch.MgpLog;
import com.soselab.microservicegraphplatform.bean.elasticsearch.RequestAndResponseMessage;
import com.soselab.microservicegraphplatform.bean.mgp.AppMetrics;
import com.soselab.microservicegraphplatform.bean.mgp.Status;
import com.soselab.microservicegraphplatform.repositories.elasticsearch.HttpRequestAndResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class LogAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(LogAnalyzer.class);

    @Autowired
    private HttpRequestAndResponseRepository httpRequestAndResponseRepository;
    @Autowired
    private ObjectMapper mapper;

    // 即時從log裡面分析去提取出來Metrics
    public AppMetrics getMetrics(String systemName, String appName, String version) {
        List<MgpLog> responseLogs = getRecentResponseLogs(systemName, appName, version, 100);
        AppMetrics metrics = new AppMetrics();
        Integer averageDuration = getAverageResponseDuration(responseLogs);
        if (averageDuration != null) {
            metrics.setAverageDuration(averageDuration);
        }
        //logger.info(systemName + ":" + appName + ":" + version + " : average duration calculate by recent " + responseLogs.size() + " responses: " + metrics.getThresholdAverageDuration() + "ms");
        metrics.setStatuses(getResponseStatusMetrics(responseLogs));
        metrics.setErrorCount(getErrorCount(systemName, appName, version));
        //logger.info(systemName + ":" + appName + ":" + version + " : error count: " + metrics.getErrorCount());
        return metrics;
    }

    public List<MgpLog> getRecentResponseLogs(String systemName, String appName, String version, int size) {
        return httpRequestAndResponseRepository.findResponseBySystemNameAndAppNameAndVersion
                (systemName, appName, version, new PageRequest(0, size, new Sort(Sort.Direction.DESC, "@timestamp")));
    }

    // 單個服務的單次回應
    public Integer getResponseDuration(MgpLog log) {
        Integer duration = null;
        try {
            RequestAndResponseMessage message = mapper.readValue(log.getMessage(), RequestAndResponseMessage.class);
            if (message != null) {
                duration = message.getDuration();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return duration;
    }

    // 單個服務的多次回應平均
    private Integer getAverageResponseDuration(List<MgpLog> logs) {
        Integer averageDuration = null;
        if (logs.size() > 0) {
            int logCount = 0;
            int durationCount = 0;
            for (MgpLog log : logs) {
                Integer duration = getResponseDuration(log);
                if (duration != null) {
                    logCount++;
                    durationCount += duration;
                }
            }
            if (durationCount > 0) {
                averageDuration = durationCount / logCount;
            }
        }
        return averageDuration;
    }

    // 提取http回應 200, 404 什麼的
    private List<Status> getResponseStatusMetrics(List<MgpLog> logs) {
        Map<Integer, Integer> statusCount = new HashMap<>();
        if (logs.size() > 0) {
            for (MgpLog log : logs) {
                RequestAndResponseMessage message = null;
                try {
                    message = mapper.readValue(log.getMessage(), RequestAndResponseMessage.class);
                    if (message != null) {
                        statusCount.merge(message.getStatus(), 1, (oldCount, newCount) -> oldCount + 1);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        List<Status> statuses = new ArrayList<>();
        statusCount.forEach((status, count) -> {
            statuses.add(new Status(status, count, (float) count/logs.size()));
        });

        return statuses;
    }

    // 服務內部Log Error層級的數量
    private Integer getErrorCount(String systemName, String appName, String version) {
        //Long result = httpRequestAndResponseRepository.findErrorBySystemNameAndAppNameAndVersion(systemName, appName, version);
        return httpRequestAndResponseRepository.findErrorBySystemNameAndAppNameAndVersion(systemName, appName, version).size();
    }

    // 計算服務在一段時間被使用的程度 （有公式）
    public Float getAppUsageMetrics(String systemName, String appName, String version, int samplingDurationMinutes) {
        List<MgpLog> requests = httpRequestAndResponseRepository.findRecentMinutesRequestBySystemNameAndAppNameAndVersion
                (systemName, appName, version, samplingDurationMinutes, new PageRequest(0, 10000, new Sort(Sort.Direction.DESC, "@timestamp")));
        // 時間沒對到 這邊先+8對應
        LocalDateTime endDateTime = LocalDateTime.now().plusHours(8);
        //System.out.println("enddatetime：" + endDateTime);
        LocalDateTime startDateTime = endDateTime.minus(samplingDurationMinutes, ChronoUnit.MINUTES);
        float usage = 0;
        for (MgpLog request : requests) {
            // 時間沒對到 這邊先+8對應
            LocalDateTime reqTime = request.getTimestamp().toLocalDateTime().plusHours(8);
            //System.out.println("reqTime：" + reqTime);
            usage += getRefreshMetrics(reqTime, startDateTime, samplingDurationMinutes);
        }
        return usage;
    }

    // 一個特定請求在這段時間內的新鮮程度, 越靠近現在越新鮮
    private Float getRefreshMetrics(LocalDateTime reqTime, LocalDateTime startTime, long samplingDuration) {
        long reqDuration = ChronoUnit.MINUTES.between(startTime, reqTime);
        return (float) reqDuration / samplingDuration;
    }

}
