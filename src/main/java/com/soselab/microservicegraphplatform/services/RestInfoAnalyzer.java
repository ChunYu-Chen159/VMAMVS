package com.soselab.microservicegraphplatform.services;

import com.soselab.microservicegraphplatform.bean.actuators.trace.Trace;
import com.soselab.microservicegraphplatform.bean.mgp.AppMetrics;
import com.soselab.microservicegraphplatform.bean.mgp.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class RestInfoAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(RestInfoAnalyzer.class);

    @Autowired
    private SpringRestTool springRestTool;

    // 從spring actuator api web 拿取
    public AppMetrics getMetrics(String systemName, String appName, String version) {
        AppMetrics metrics = new AppMetrics();
        List<Trace> traces = springRestTool.getTracesFromRemoteApp(systemName, appName, version);
        Integer averageDuration = getAverageResponseDuration(traces);
        metrics.setAverageDuration(averageDuration);
        metrics.setStatuses(getResponseStausMetrics(traces));
        return metrics;
    }

    private Integer getAverageResponseDuration(List<Trace> traces) {
        long durationCount = 0;
        for (Trace trace : traces) {
            if (trace.getInfo().getTimeTaken() != null) {
                durationCount += Long.parseLong(trace.getInfo().getTimeTaken());
            }
        }
        return (int) durationCount / traces.size();
    }

    private List<Status> getResponseStausMetrics(List<Trace> traces) {
        Map<Integer, Integer> statusCount = new HashMap<>();
        for (Trace trace : traces) {
            statusCount.merge(Integer.parseInt(trace.getInfo().getHeaders().getResponse().getStatus()), 1, (oldCount, newCount) -> oldCount + 1);
        }

        List<Status> statuses = new ArrayList<>();
        statusCount.forEach((status, count) -> {
            statuses.add(new Status(status, count, (float) count/traces.size()));
        });
        return statuses;
    }

}
