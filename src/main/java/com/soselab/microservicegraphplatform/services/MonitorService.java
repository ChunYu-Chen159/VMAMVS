package com.soselab.microservicegraphplatform.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soselab.microservicegraphplatform.bean.elasticsearch.MgpLog;
import com.soselab.microservicegraphplatform.bean.mgp.AppMetrics;
import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.*;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;
import com.soselab.microservicegraphplatform.bean.mgp.notification.warning.*;
import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import com.soselab.microservicegraphplatform.bean.neo4j.Setting;
import com.soselab.microservicegraphplatform.controllers.WebPageController;
import com.soselab.microservicegraphplatform.repositories.neo4j.EndpointRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.GeneralRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.LinkRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.net.ntp.TimeStamp;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@Configuration
public class MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Autowired
    private GeneralRepository generalRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private EndpointRepository endpointRepository;
    @Autowired
    private LinkRepository linkRepository;
    @Autowired
    private LogAnalyzer logAnalyzer;
    @Autowired
    private RestInfoAnalyzer restInfoAnalyzer;
    @Autowired
    private WebPageController webPageController;
    @Autowired
    private WebNotificationService notificationService;
    @Autowired
    private SleuthService sleuthService;
    @Autowired
    private SpringRestTool springRestTool;
    @Autowired
    private ObjectMapper mapper;

    private Map<String, SpcData> failureStatusRateSPCMap = new HashMap<>();
    private Map<String, SpcData> averageDurationSPCMap = new HashMap<>();


    private final int STATUSCODE500 = 500;
    private final int STATUSCODE502 = 502;
    private final int STATUSCODE503 = 503;
    private final int STATUSCODE504 = 504;

    private Map<String, List<MonitorError>> allMonitorErrorList = new HashMap<>();

    @Scheduled(cron = "0 0 3 1/1 * ?") // 週期執行
    private void everyDayScheduled() {
        List<String> systemNames = generalRepository.getAllSystemName();
        for (String systemName : systemNames) {
            List<Service> services = serviceRepository.findBySystemNameWithOptionalSettingNotNull(systemName);
            checkLowUsageVersionAlert(systemName, services, 1440);
        }
        logger.info("Daily scheduled executed");
    }

    public void runScheduled(String systemName) {
        List<Service> services = serviceRepository.findBySystemNameWithOptionalSettingNotNull(systemName);
        updateSPCData(systemName, services);
        checkUserAlert(systemName, services);
        //checkSPCAlert(systemName); // 這個沒有做按鈕來開啟關閉，超過就會發通知
    }

    @Scheduled(fixedDelay = 3600000) //每小時執行
    public void hourScheduled(){
        List<String> systemNames = generalRepository.getAllSystemName();
        for (String systemName : systemNames) {
            checkErrorFromSleuth(systemName);
        }
        logger.info("Hourly scheduled executed");
    }

    // 抓錯誤
    public void checkErrorFromSleuth(String systemName){
        List<Service> ServicesInDB = serviceRepository.findBySysName(systemName);
        Long nowTime = System.currentTimeMillis();
        // 1天
//        Long lookback = 1 * 24 * 60 * 60 * 1000L;
        // 1小時
        Long lookback = 1 * 60 * 60 * 1000L;
        int limit = 10000;


        for(Service s : ServicesInDB) {

            serviceRepository.setMonitorErrorConditionByAppId(s.getAppId(), "FALSE");

            Long endTime = nowTime;
            String jsonContent_500 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE500, lookback, endTime, limit);
            String jsonContent_502 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE502, lookback, endTime, limit);
            String jsonContent_503 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE503, lookback, endTime, limit);
            String jsonContent_504 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE504, lookback, endTime, limit);

            System.out.println("jsonContent_500: " + jsonContent_500);
            System.out.println("jsonContent_502: " + jsonContent_502);
            System.out.println("jsonContent_503: " + jsonContent_503);
            System.out.println("jsonContent_504: " + jsonContent_504);

            JSONArray array500 = new JSONArray(jsonContent_500);
            JSONArray array502 = new JSONArray(jsonContent_502);
            JSONArray array503 = new JSONArray(jsonContent_503);
            JSONArray array504 = new JSONArray(jsonContent_504);

            List<MonitorError> monitorErrorList500 = analyzeError(array500, systemName);
            List<MonitorError> monitorErrorList502 = analyzeError(array502, systemName);
            List<MonitorError> monitorErrorList503 = analyzeError(array503, systemName);
            List<MonitorError> monitorErrorList504 = analyzeError(array504, systemName);

            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrorList500),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrorList500));
            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrorList502),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrorList502));
            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrorList503),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrorList503));
            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrorList504),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrorList504));

        }

        List<MonitorError> monitorErrors = allMonitorErrorList.getOrDefault(systemName, null);

        if(monitorErrors != null){
            // 確認錯誤時間是否早於測試時間 （錯過之後有進行測試，然後有過）
            // 倒序刪除，不然會影響前面元素
            for(int i = monitorErrors.size() - 1; i >= 0; i--){
                MonitorError monitorError = monitorErrors.get(i);
                System.out.println("111111111111111111111111111111");
                Map<String, Object> swaggerMap = springRestTool.getSwaggerFromRemoteApp2(monitorError.getErrorSystemName(), monitorError.getErrorAppName(), monitorError.getErrorAppVersion());
                if (swaggerMap != null) {
                    System.out.println("2222222222222222222222222222222");
                    Map<String, Object> contractsMap = mapper.convertValue(swaggerMap.get("x-contract"), new TypeReference<Map<String, Object>>() {});
                    Map<String, Object> groovyMap = mapper.convertValue(contractsMap.get(monitorError.getConsumerAppName().toLowerCase() + ".groovy"), new TypeReference<Map<String, Object>>() {});
                    for (Map.Entry<String, Object> entry : groovyMap.entrySet()) {
                        System.out.println("333333333333333333333333333333");
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if(key.split("_")[0].equals(monitorError.getErrorPath())){
                            System.out.println("444444444444444444444444444");
                            Map<String, Object> apiMap = mapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
                            Map<String, Object> testResultMap = mapper.convertValue(apiMap.get("testResult"), new TypeReference<Map<String, Object>>() {});
                            String status = mapper.convertValue(testResultMap.get("status"), new TypeReference<String>() {});
                            if (status.equals("PASS")) {
                                System.out.println("55555555555555555555555555555555555");
                                String time = mapper.convertValue(testResultMap.get("finished-at"), new TypeReference<String>() {});
                                System.out.println("time: " + time);
                                time = time.replaceAll("T"," ").replaceAll("Z","");
                                System.out.println("time: " + time);
                                Long testTime = Timestamp.valueOf(time).getTime();

                                System.out.println("testTime: " + testTime);
                                System.out.println("monitorError.getTimestamp(): " + monitorError.getTimestamp());
                                System.out.println("monitorError.getTimestamp()/1000: " + monitorError.getTimestamp()/1000);

                                if(testTime > (monitorError.getTimestamp()/1000)){
                                    System.out.println("66666666666666666666666666666666666");
                                    serviceRepository.setMonitorErrorConditionByAppId(monitorError.getErrorAppId(), "FALSE");
                                    monitorErrors.remove(monitorErrors.indexOf(monitorError));
                                }

                            }
                        }
                    }
                }
            }

            allMonitorErrorList.remove(systemName);
            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrors),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrors));


        }





    }

    public List<MonitorError> analyzeError(JSONArray array, String systemName) {

        List<MonitorError> monitorErrorList = new ArrayList<>();

        for(int i = 0; i < array.length(); i++) { // 每個error
            JSONArray array_everyError = array.getJSONArray(i);
            MonitorError monitorError = new MonitorError();
            ArrayList<ErrorService> es = new ArrayList<>();
            ArrayList<ErrorEndpoint> ee = new ArrayList<>();
            ArrayList<ErrorLink> el = new ArrayList<>();
            long timestamp = 0;
            String statusCode = "";
            String errorMessage = "";
            String errorAppName = "";
            String errorAppVersion = "";
            String errorPath = "";
            String errorUrl = "";
            String errorMethod = "";
            String consumerAppName = "";

            boolean check4XX = false;
            boolean checkSwagger =false;

            for(int j = 0; j < array_everyError.length(); j++) {
                if(array_everyError.getJSONObject(j).getString("kind").equals("SERVER")) {
                    JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");
                    if (!jsonObject.has("http.appName") || !jsonObject.has("http.version")) {
                        check4XX = true;
                        break;
                    }
                }
                if(array_everyError.getJSONObject(j).getString("name").equals("http:/v2/api-docs")) {
                    checkSwagger = true;
                    break;
                }
            }

            if(check4XX || checkSwagger)
                break;


            // 每個Service, Endpoint, OwnLink (httpRequest關係還未加入)
            for(int j = 0; j < array_everyError.length(); j++){


                if(array_everyError.getJSONObject(j).getString("kind").equals("SERVER")){
                    JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");

                    if(!jsonObject.has("http.appName") || !jsonObject.has("http.version"))
                        break;
                    String appName = jsonObject.getString("http.appName").toUpperCase();
                    String version = jsonObject.getString("http.version").toUpperCase();
                    String appId = systemName.toUpperCase() + ":" + appName + ":" + version;
                    long serviceId = serviceRepository.findServiceIdByAppId(appId);
                    String endpointPath = "";

                    if(array_everyError.getJSONObject(j).getJSONObject("tags").has("http.path")) {
                        endpointPath = array_everyError.getJSONObject(j).getJSONObject("tags").getString("http.path");
                    }else{
                        String serverId = array_everyError.getJSONObject(j).getString("id");
                        for(int k = 0; k < array_everyError.length(); k++){
                            if (array_everyError.getJSONObject(k).getString("kind").equals("CLIENT")) {
                                String clientId = array_everyError.getJSONObject(k).getString("id");
                                if (serverId.equals(clientId)) {
                                    JSONObject jsonObject2 = array_everyError.getJSONObject(k).getJSONObject("tags");
                                    endpointPath = jsonObject2.getString("http.path");
                                }
                            }
                        }
                    }

                    long endpointId = endpointRepository.findIdByAppIdAndEnpointPath(appId,endpointPath);

                    long linkId = linkRepository.findLinkIdBySystemNameAndAidAndBidWithOwn(systemName.toUpperCase(), serviceId, endpointId);


                    es.add(new ErrorService(serviceId, appName, version, appId));
                    ee.add(new ErrorEndpoint(endpointId, appId, appName, endpointPath));
                    el.add(new ErrorLink(linkId, serviceId, "OWN", endpointId));

                }
            }

            // httpRequestLink
            for(int j = 0; j < array_everyError.length(); j++){
                if(array_everyError.getJSONObject(j).getString("kind").equals("SERVER")){
                    if(array_everyError.getJSONObject(j).has("shared")) {
                        if (array_everyError.getJSONObject(j).getBoolean("shared")) {
                            String serverId = array_everyError.getJSONObject(j).getString("id");
                            JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("localEndpoint");
                            String serverName = jsonObject.getString("serviceName");
                            long endpointId = 0;
                            for (ErrorEndpoint e : ee) {
                                if (e.getParentAppName().equals(serverName.toUpperCase())) {
                                    endpointId = e.getId();
                                }
                            }

                            for (int k = 0; k < array_everyError.length(); k++) {
                                if (array_everyError.getJSONObject(k).getString("kind").equals("CLIENT")) {
                                    String clientId = array_everyError.getJSONObject(k).getString("id");
                                    if (serverId.equals(clientId)) {
                                        JSONObject jsonObject2 = array_everyError.getJSONObject(k).getJSONObject("localEndpoint");

                                        String clientName = jsonObject2.getString("serviceName");

                                        for (ErrorEndpoint e : ee) {
                                            if (e.getParentAppName().equals(clientName.toUpperCase())) {
                                                long endpointId2 = e.getId();
                                                long linkId = linkRepository.findLinkIdBySystemNameAndAidAndBidWithHttpRequest(systemName.toUpperCase(), endpointId2, endpointId);
                                                el.add(new ErrorLink(linkId, endpointId2, "HTTP_REQUEST", endpointId));
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }

            // errorAppName, errorAppVersion, errorMessage, statusCode, timestamp, errorPath, consumerAppName
            for(int j = 0; j < array_everyError.length(); j++){
                if(array_everyError.getJSONObject(j).has("shared")) {
                    if (array_everyError.getJSONObject(j).getBoolean("shared")) {
                        JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");
                        String serverId = array_everyError.getJSONObject(j).getString("id");
                        if (jsonObject.has("error")) {
                            errorAppName = jsonObject.getString("http.appName");
                            errorAppVersion = jsonObject.getString("http.version");
                            errorMessage = jsonObject.getString("error");
                            statusCode = jsonObject.getString("http.status_code");
                            timestamp = array_everyError.getJSONObject(j).getLong("timestamp");

                            for (int k = 0; k < array_everyError.length(); k++) {
                                if (array_everyError.getJSONObject(k).getString("kind").equals("CLIENT")) {
                                    String clientId = array_everyError.getJSONObject(k).getString("id");
                                    if (serverId.equals(clientId)) {
                                        JSONObject jsonObject2 = array_everyError.getJSONObject(k).getJSONObject("tags");
                                        errorPath = jsonObject2.getString("http.path");
                                        errorUrl = jsonObject2.getString("http.url");
                                        errorMethod = jsonObject2.getString("http.method");
                                        JSONObject jsonObject3 = array_everyError.getJSONObject(k).getJSONObject("localEndpoint");
                                        consumerAppName = jsonObject3.getString("serviceName");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            monitorError.setErrorAppId(systemName.toUpperCase() + ":" + errorAppName.toUpperCase() + ":" + errorAppVersion);
            monitorError.setErrorSystemName(systemName);
            monitorError.setErrorAppName(errorAppName);
            monitorError.setErrorAppVersion(errorAppVersion);
            monitorError.setConsumerAppName(consumerAppName);
            monitorError.setErrorMessage(errorMessage);
            monitorError.setStatusCode(statusCode);
            monitorError.setTimestamp(timestamp);
            monitorError.setErrorPath(errorPath);
            monitorError.setErrorUrl(errorUrl);
            monitorError.setErrorMethod(errorMethod);
            monitorError.setErrorServices(es);
            monitorError.setErrorEndpoints(ee);
            monitorError.setErrorLinks(el);


            monitorErrorList.add(monitorError);


        }


        return monitorErrorList;
    }

    private List<MonitorError> pushMonitorError(List<MonitorError> monitorErrors, List<MonitorError> monitorErrors2) {
        /*if (monitorErrors.size() == 100) {
            monitorErrors.remove(99);
        } else {*/
            /*List<MonitorError> temp = new ArrayList(monitorErrors);
            // 存共同有的數據
            temp.retainAll(monitorErrors2);
            // 去除共同有的數據
            monitorErrors2.removeAll(temp); 這段不要*/
            // 加回去
           /* monitorErrors.addAll(0, monitorErrors2);
        }*/

        monitorErrors.addAll(0, monitorErrors2);

        for(MonitorError monitorError : monitorErrors) {
            monitorError.setIndex(monitorErrors.indexOf(monitorError));
            serviceRepository.setMonitorErrorConditionByAppId(monitorError.getErrorAppId(), "TRUE");
        }

        return monitorErrors;
    }

    public List<MonitorError> getErrorsOfSystem(String systemName) {
        return allMonitorErrorList.getOrDefault(systemName, new ArrayList<>());
    }


    // 確認服務有沒有出現異常
    public void checkUserAlert(String systemName, List<Service> services) {
        for (Service service : services) {
            Setting setting = service.getSetting();
            if (setting == null) {
                continue;
            }
            // Using log (Elasticsearch) metrics
            if (setting.getEnableLogFailureAlert() || setting.getEnableLogAverageDurationAlert()) {
                AppMetrics metrics = logAnalyzer.getMetrics(service.getSystemName(), service.getAppName(), service.getVersion());
                if (setting.getEnableLogFailureAlert()) {
                    // Failure status rate
                    Pair<Boolean, Float> failureStatusRateResult = isFailureStatusRateExceededThreshold
                            (metrics, setting.getFailureStatusRate());
                    if (failureStatusRateResult.getKey()) {
                        WebNotification notification = new FailureStatusRateWarningNotification(service.getAppName(),
                                service.getVersion(), failureStatusRateResult.getValue(), setting.getFailureStatusRate(),
                                FailureStatusRateWarningNotification.DATA_ELASTICSEARCH, FailureStatusRateWarningNotification.THRESHOLD_USER);
                        notificationService.pushNotificationToSystem(systemName, notification);
                    }
                    // Error
                    if (setting.getFailureErrorCount() != null && metrics.getErrorCount() > setting.getFailureErrorCount()) {
                        WebNotification notification = new FailureErrorNotification(service.getAppName(), service.getVersion(),
                                metrics.getErrorCount(), setting.getFailureErrorCount(), FailureErrorNotification.TYPE_ELASTICSEARCH);
                        notificationService.pushNotificationToSystem(systemName, notification);
                        logger.info("Found service " + service.getAppId() + " exception: error count = " +
                                metrics.getErrorCount() + " (threshold = " + setting.getFailureErrorCount() + ")");
                    }
                }
                if (setting.getEnableLogAverageDurationAlert()) {
                    if (setting.getThresholdAverageDuration() != null && metrics.getAverageDuration() > setting.getThresholdAverageDuration()) {
                        WebNotification notification = new HighAvgDurationNotification(service.getAppName(), service.getVersion(),
                                metrics.getAverageDuration(), setting.getThresholdAverageDuration(), HighAvgDurationNotification.DATA_ELASTICSEARCH);
                        notificationService.pushNotificationToSystem(systemName, notification);
                    }
                }
            }
            // Using rest (Spring Actuator) metrics
            if (setting.getEnableRestFailureAlert() || setting.getEnableRestAverageDurationAlert()) {
                AppMetrics metrics = restInfoAnalyzer.getMetrics(service.getSystemName(), service.getAppName(), service.getVersion());
                // Failure status rate
                if (setting.getEnableRestFailureAlert()) {
                    Pair<Boolean, Float> failureStatusRateResult = isFailureStatusRateExceededThreshold
                            (metrics, setting.getFailureStatusRate());
                    if (failureStatusRateResult.getKey()) {
                        WebNotification notification = new FailureStatusRateWarningNotification(service.getAppName(),
                                service.getVersion(), failureStatusRateResult.getValue(), setting.getFailureStatusRate(),
                                FailureStatusRateWarningNotification.DATA_ACTUATOR, FailureStatusRateWarningNotification.THRESHOLD_USER);
                        notificationService.pushNotificationToSystem(systemName, notification);
                    }
                }
                if (setting.getEnableRestAverageDurationAlert()) {
                    if (setting.getThresholdAverageDuration() != null && metrics.getAverageDuration() > setting.getThresholdAverageDuration()) {
                        WebNotification notification = new HighAvgDurationNotification(service.getAppName(), service.getVersion(),
                                metrics.getAverageDuration(), setting.getThresholdAverageDuration(), HighAvgDurationNotification.DATA_ACTUATOR);
                        notificationService.pushNotificationToSystem(systemName, notification);
                    }
                }
            }
            // Using SPC
            if (setting.getEnableSPCHighDurationRateAlert()) {
                SpcData spcData = getAppDurationSPC(service.getAppId());
                int violationCount = 0;
                for (Map.Entry<String, Float> entry : spcData.getValues().entrySet()) {
                    if (entry.getValue() > spcData.getUcl()) {
                        violationCount++;
                    }
                }
                float highDurationRate = (float) violationCount / spcData.getValues().size();
                if (highDurationRate > setting.getThresholdSPCHighDurationRate()) {
                    WebNotification notification = new SpcHighDurationRateNotification(service.getAppName(), service.getVersion(),
                            highDurationRate, setting.getThresholdSPCHighDurationRate());
                    notificationService.pushNotificationToSystem(systemName, notification);
                }
            }
            // Using Risk
            if (setting.getEnableRiskValueAlert()) {
                if (serviceRepository.getRiskValueByAppId(service.getAppId()) > setting.getRiskValueAlert())  {
                    WebNotification notification = new HighRiskValueNotification(service.getAppName(), service.getVersion(),
                            serviceRepository.getRiskValueByAppId(service.getAppId()), setting.getRiskValueAlert());
                    notificationService.pushNotificationToSystem(systemName, notification);
                    serviceRepository.setHighRiskConditionByAppId(service.getAppId(),"TRUE");
                }else {
                    serviceRepository.setHighRiskConditionByAppId(service.getAppId(),"FALSE");
                }
            }else {
                serviceRepository.setHighRiskConditionByAppId(service.getAppId(),"FALSE");
            }
        }
    }

    // Pair<isExceededThreshold, failureStatusRate>
    private Pair<Boolean, Float> isFailureStatusRateExceededThreshold(AppMetrics metrics, float threshold) {
        float failureStatusRate = metrics.getFailureStatusRate();
        return new ImmutablePair<>(failureStatusRate > threshold, failureStatusRate);
    }

    // Follow codes are for SPC
    private float getPChartSD(float cl, float n) {
        return (float) Math.sqrt(cl*(1-cl)/n);
    }

    private float getUChartSD(float cl, float n) {
        return (float) Math.sqrt(cl/n);
    }

    private float getCChartSD(float cl) {
        return (float) Math.sqrt(cl);
    }

    private void checkSPCAlert(String systemName) {
        SpcData spcData = failureStatusRateSPCMap.get(systemName);
        if (spcData != null) {
            float ucl = spcData.getUcl();
            spcData.getValues().forEach((app, value) -> {
                if (value > ucl) {
                    String appName = app.split(":")[0];
                    String version = app.split(":")[1];
                    notificationService.pushNotificationToSystem(systemName, new FailureStatusRateWarningNotification(appName, version, value, ucl,
                            FailureStatusRateWarningNotification.DATA_ELASTICSEARCH, FailureStatusRateWarningNotification.THRESHOLD_SPC));
                }
            });
        }
    }

    private void updateSPCData(String systemName, List<Service> services) {
        Map<String, AppMetrics> metricsMap = new HashMap<>();
        for (Service service : services) {
            metricsMap.put(service.getAppName() + ":" + service.getVersion(), logAnalyzer.getMetrics(service.getSystemName(), service.getAppName(), service.getVersion()));
        }
        SpcData failureStatusRateSpcData = getNowFailureStatusRateSPC(metricsMap);
        webPageController.sendAppsFailureStatusRateSPC(systemName, failureStatusRateSpcData);
        failureStatusRateSPCMap.put(systemName, failureStatusRateSpcData);

        SpcData durationSpcData = getNowAverageDurationSPC(metricsMap);
        webPageController.sendAppsAverageDurationSPC(systemName, durationSpcData);
        averageDurationSPCMap.put(systemName, durationSpcData);

    }

    // P chart
    private SpcData getNowFailureStatusRateSPC(Map<String, AppMetrics> metricsMap) {
        float valueCount = 0;
        int sampleGroupsNum = 0;
        long samplesCount = 0;
        Map<String, Float> values = new HashMap<>();
        for (Map.Entry<String, AppMetrics> entry : metricsMap.entrySet()) {
            String app = entry.getKey();
            AppMetrics metrics = entry.getValue();
            int samplesNum = metrics.getFailureStatusSamplesNum();
            if (samplesNum > 0) {
                float value = metrics.getFailureStatusRate();
                valueCount += value;
                sampleGroupsNum ++;
                samplesCount += samplesNum;
                values.put(app, value);
            }
        }
        float cl = valueCount / sampleGroupsNum;
        float n = (float) samplesCount/sampleGroupsNum;
        float sd = getPChartSD(cl, n);
        float ucl = cl + 3*sd;
        float lcl = cl - 3*sd;
        if (lcl < 0) {
            lcl = 0;
        }
        return new SpcData(cl, ucl, lcl, values, "Failure Status Rate", "Services", new ArrayList<>(Collections.singletonList(SpcData.UCL)));
    }

    public SpcData getFailureStatusRateSPC(String systemName) {
        return failureStatusRateSPCMap.get(systemName);
    }

    // U chart
    private SpcData getNowAverageDurationSPC(Map<String, AppMetrics> metricsMap) {
        float valueCount = 0;
        int sampleGroupsNum = 0;
        long samplesCount = 0;
        Map<String, Float> values = new HashMap<>();
        for (Map.Entry<String, AppMetrics> entry : metricsMap.entrySet()) {
            String app = entry.getKey();
            AppMetrics metrics = entry.getValue();
            int samplesNum = metrics.getDurationSamplesNum();
            if (samplesNum > 0) {
                float value = metrics.getAverageDuration();
                valueCount += value;
                sampleGroupsNum ++;
                samplesCount += samplesNum;
                values.put(app, value);
            }
        }
        float cl = valueCount / sampleGroupsNum;
        float n = (float) samplesCount/sampleGroupsNum;
        float sd = getUChartSD(cl, n);
        float ucl = cl + 3*sd;
        float lcl = cl - 3*sd;
        if (lcl < 0) {
            lcl = 0;
        }
        return new SpcData(cl, ucl, lcl, values, "Average Duration", "Services", new ArrayList<>(Collections.singletonList(SpcData.UCL)));
    }

    public SpcData getAverageDurationSPC(String systemName) {
        return averageDurationSPCMap.get(systemName);
    }

    // C chart
    public SpcData getAppDurationSPC(String appId) {
        String[] appInfo = appId.split(":");
        String systemName = appInfo[0];
        String appName = appInfo[1];
        String version = appInfo[2];
        List<MgpLog> logs = logAnalyzer.getRecentResponseLogs(systemName, appName, version, 100);
        float valueCount = 0;
        int samplesNum = 0;
        Map<String, Float> values = new LinkedHashMap<>();
        for (MgpLog log : logs) {
            Integer duration = logAnalyzer.getResponseDuration(log);
            if (duration != null) {
                String time = dateFormat.format(log.getTimestamp());
                valueCount += duration;
                samplesNum ++;
                values.put(time, (float) duration);
            }
        }
        float cl = valueCount / samplesNum;
        float sd = getCChartSD(cl);
        float ucl = cl + 3*sd;
        float lcl = cl - 3*sd;
        if (lcl < 0) {
            lcl = 0;
        }
        return new SpcData(cl, ucl, lcl, values, "Duration", appName + ":" + version,
                new ArrayList<>(Collections.singletonList(SpcData.UCL)));
    }

    // Find low-usage version of apps
    private void checkLowUsageVersionAlert(String systemName, List<Service> services, int samplingDurationMinutes) {
        Map<String, Set<String>> appNameAndVerSetMap = new HashMap<>();
        for (Service service : services) {
            appNameAndVerSetMap.merge(service.getAppName(), new HashSet<>(Arrays.asList(service.getVersion())),
                    (oldSet, newSet) -> {
                oldSet.add(service.getVersion());
                return oldSet;
            });
        }
        appNameAndVerSetMap.forEach((appName, versions) -> {
            SpcData usageSpc = createVersionUsageSPC(systemName, appName, versions, samplingDurationMinutes);
            //logger.info(systemName + ":" + appName + " usage SPC, CL = " + usageSpc.getCl() + " UCL = " + usageSpc.getUcl() + " LCL = " + usageSpc.getLcl());
            usageSpc.getValues().forEach((version, usageMetrics) -> {
                //logger.info(systemName + ":" + appName + ":" + version + " usage metrics: " + usageMetrics);
                if (usageMetrics < usageSpc.getLcl()) {
                    notificationService.pushNotificationToSystem(systemName, new LowUsageVersionNotification(appName, version));
                    //logger.info("Found low usage version service: " + systemName + ":" + appName + ":" + version);
                }
            });
        });
    }

    private SpcData createVersionUsageSPC(String systemName, String appName, Set<String> versions, int samplingDurationMinutes) {
        float valueCount = 0;
        int samplesNum = versions.size();
        Map<String, Float> values = new HashMap<>();
        for (String version : versions) {
            float usageMetrics = logAnalyzer.getAppUsageMetrics(systemName, appName, version, samplingDurationMinutes);
            valueCount += usageMetrics;
            values.put(version, usageMetrics);
        }
        float cl = valueCount / samplesNum;
        float sd = getCChartSD(cl);
        float ucl = cl + 3*sd;
        float lcl = cl - 3*sd;
        if (lcl < 0) {
            lcl = 0;
        }
        return new SpcData(cl, ucl, lcl, values, "Usage", appName, new ArrayList<>(Collections.singletonList(SpcData.LCL)));
    }

    public SpcData getVersionUsageSPC(String appId) {
        String[] appInfo = appId.split(":");
        String systemName = appInfo[0];
        String appName = appInfo[1];
        List<Service> services = serviceRepository.findAllVersInSameSysBySysNameAndAppName(systemName, appName);
        Set<String> versions = new HashSet<>();
        for (Service service : services) {
            versions.add(service.getVersion());
        }
        return createVersionUsageSPC(systemName, appName, versions, 60);
    }

}
