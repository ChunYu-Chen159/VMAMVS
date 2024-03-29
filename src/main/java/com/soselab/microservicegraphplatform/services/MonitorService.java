package com.soselab.microservicegraphplatform.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soselab.microservicegraphplatform.bean.elasticsearch.MgpLog;
import com.soselab.microservicegraphplatform.bean.mgp.AppMetrics;
import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.*;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.chart.TotalErrorChart;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Configuration
public class MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");

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
    @Autowired
    private MonitorErrorSimulator monitorErrorSimulator;
    @Autowired
    private ContractService contractService;
    @Autowired
    private RiskService riskService;

    private Map<String, SpcData> failureStatusRateSPCMap = new HashMap<>();
    private Map<String, SpcData> averageDurationSPCMap = new HashMap<>();


    private final int STATUSCODE500 = 500;
    private final int STATUSCODE502 = 502;
    private final int STATUSCODE503 = 503;
    private final int STATUSCODE504 = 504;

    private Map<String, List<MonitorError>> allMonitorErrorList = new HashMap<>();
    private Map<String, List<MonitorError>> allSimulateMonitorErrorList = new HashMap<>();

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
    public void hourScheduled() {
        List<String> systemNames = generalRepository.getAllSystemName();
        for (String systemName : systemNames) {
            checkErrorFromSleuth(systemName);
            contractService.setAllServiceContractTestingCondition(systemName);
            contractService.setAllServiceContractMissingCondition(systemName);
            riskService.setServiceRisk(systemName);
        }
        logger.info("Hourly scheduled executed");
    }

    // 抓錯誤
    public void checkErrorFromSleuth(String systemName) {
        List<Service> ServicesInDB = serviceRepository.findBySysName(systemName);
        Long nowTime = System.currentTimeMillis();
        // 1天
//        Long lookback = 1 * 24 * 60 * 60 * 1000L;
        // 1小時
        Long lookback = 1 * 60 * 60 * 1000L;
        int limit = 10000;

        // 以下為實際分析錯誤使用的方法
        for(Service s : ServicesInDB) {

            serviceRepository.setMonitorErrorConditionByAppId(s.getAppId(), "FALSE");

            System.out.println("service: : " + s.getAppName());

            Long endTime = nowTime;

            String jsonContent_500 = "[]";
            String jsonContent_502 = "[]";
            String jsonContent_503 = "[]";
            String jsonContent_504 = "[]";

            try {
                jsonContent_500 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE500, lookback, endTime, limit);
                jsonContent_502 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE502, lookback, endTime, limit);
                jsonContent_503 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE503, lookback, endTime, limit);
                jsonContent_504 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE504, lookback, endTime, limit);
            }catch(NullPointerException e){
                e.printStackTrace();
            }

            System.out.println("jsonContent_500: " + jsonContent_500);
            System.out.println("jsonContent_502: " + jsonContent_502);
            System.out.println("jsonContent_503: " + jsonContent_503);
            System.out.println("jsonContent_504: " + jsonContent_504);

            JSONArray array500 = new JSONArray(jsonContent_500);
            JSONArray array502 = new JSONArray(jsonContent_502);
            JSONArray array503 = new JSONArray(jsonContent_503);
            JSONArray array504 = new JSONArray(jsonContent_504);

            System.out.println("analyzeError: ");

            List<MonitorError> monitorErrorList500 = analyzeError(array500, systemName, s.getAppName(), s.getVersion());
            List<MonitorError> monitorErrorList502 = analyzeError(array502, systemName, s.getAppName(), s.getVersion());
            List<MonitorError> monitorErrorList503 = analyzeError(array503, systemName, s.getAppName(), s.getVersion());
            List<MonitorError> monitorErrorList504 = analyzeError(array504, systemName, s.getAppName(), s.getVersion());


            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrorList500),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrorList500));
            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrorList502),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrorList502));
            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrorList503),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrorList503));
            allMonitorErrorList.merge(systemName, new ArrayList<>(monitorErrorList504),
                    (oldList, newList) -> pushMonitorError(oldList, monitorErrorList504));

        }


        // 以下為模擬錯誤使用的方法
        /*List<MonitorError> simulatorMonitorErrors = monitorErrorSimulator.simulateErrors(systemName);
        allMonitorErrorList.merge(systemName, new ArrayList<>(simulatorMonitorErrors),
                (oldList, newList) -> pushMonitorError(oldList, simulatorMonitorErrors));
        allSimulateMonitorErrorList.remove(systemName);
        allSimulateMonitorErrorList.put(systemName, simulatorMonitorErrors);*/
        // ------------------------------------------------

        if(!allMonitorErrorList.get(systemName).isEmpty() && allMonitorErrorList.get(systemName).size() > 0){
            checkTimeOfTestAndMonitorError(allMonitorErrorList.get(systemName));
            checkTestedPASS_MonitorError(allMonitorErrorList.get(systemName));
            setMonitorErrorCondition(allMonitorErrorList.get(systemName));
        }


    }


    // 確認錯誤時間是否早於測試時間 （錯過之後有進行測試，然後有過）
    public void checkTimeOfTestAndMonitorError(List<MonitorError> monitorErrors) {

        // 倒序刪除，不然會影響前面元素
        for(int i = monitorErrors.size() - 1; i >= 0; i--){
            MonitorError monitorError = monitorErrors.get(i);
            if(!monitorError.getErrorType().equals("RequestError")) {
                Map<String, Object> swaggerMap = springRestTool.getSwaggerFromRemoteApp2(monitorError.getErrorSystemName(), monitorError.getErrorAppName(), monitorError.getErrorAppVersion());
                if (swaggerMap != null) {
                    Map<String, Object> contractsMap = mapper.convertValue(swaggerMap.get("x-contract"), new TypeReference<Map<String, Object>>() {
                    });
                    Map<String, Object> groovyMap = mapper.convertValue(contractsMap.get(monitorError.getConsumerAppName().toLowerCase() + ".groovy"), new TypeReference<Map<String, Object>>() {
                    });

                    boolean hasRemoved = false;
                    for (Map.Entry<String, Object> entry : groovyMap.entrySet()) {
                        if(hasRemoved)
                            break;
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if (key.equals(monitorError.getErrorPath())) {

                            try {

                                String jsonStr = mapper.writeValueAsString(value);
                                JSONArray jsonArr = new JSONArray(jsonStr);

                                for (int j = 0; j < jsonArr.length(); j++) {
                                    String status = jsonArr.getJSONObject(j).getJSONObject("testResult").getString("status");

                                    if (status.equals("PASS")) {
                                        String time = jsonArr.getJSONObject(j).getJSONObject("testResult").getString("finished_at");

                                        try {

                                            Date date1 = dateFormat2.parse(time);
                                            String str = dateFormat2.format(monitorError.getTimestamp() / 1000L);
                                            Date date2 = dateFormat2.parse(str);

                                            Calendar cal1 = Calendar.getInstance();
                                            Calendar cal2 = Calendar.getInstance();
                                            cal1.setTime(date1);
                                            cal2.setTime(date2);

                                            if (cal1.after(cal2)) {
                                                serviceRepository.setMonitorErrorConditionByAppId(monitorError.getErrorAppId(), "FALSE");
                                                monitorErrors.remove(monitorErrors.indexOf(monitorError));
                                                hasRemoved = true;
                                                break;
                                            }
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }else {
                try {
                    // 5天內
                    String str = dateFormat2.format(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L);
                    Date date1 = dateFormat2.parse(str);
                    String str2 = dateFormat2.format(monitorError.getTimestamp() / 1000L);
                    Date date2 = dateFormat2.parse(str2);

                    Calendar cal1 = Calendar.getInstance();
                    Calendar cal2 = Calendar.getInstance();
                    cal1.setTime(date1);
                    cal2.setTime(date2);

                    if (cal1.after(cal2)) {
                        serviceRepository.setMonitorErrorConditionByAppId(monitorError.getErrorAppId(), "FALSE");
                        monitorErrors.remove(monitorErrors.indexOf(monitorError));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    // 確認監控到的錯誤是否測試有通過
    public void checkTestedPASS_MonitorError(List<MonitorError> monitorErrors) {

        for(int i = monitorErrors.size() - 1; i >= 0; i--){
            MonitorError monitorError = monitorErrors.get(i);

            if(!monitorError.getErrorType().equals("RequestError")) {
                Map<String, Object> swaggerMap = springRestTool.getSwaggerFromRemoteApp2(monitorError.getErrorSystemName(), monitorError.getErrorAppName(), monitorError.getErrorAppVersion());
                if (swaggerMap != null) {

                    Map<String, Object> contractsMap = mapper.convertValue(swaggerMap.get("x-contract"), new TypeReference<Map<String, Object>>() {
                    });
                    Map<String, Object> groovyMap = mapper.convertValue(contractsMap.get(monitorError.getConsumerAppName().toLowerCase() + ".groovy"), new TypeReference<Map<String, Object>>() {
                    });

                    for (Map.Entry<String, Object> entry : groovyMap.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        if (key.equals(monitorError.getErrorPath())) {

                            try {

                                String jsonStr = mapper.writeValueAsString(value);
                                JSONArray jsonArr = new JSONArray(jsonStr);

                                for (int j = 0; j < jsonArr.length(); j++) {
                                    String status = jsonArr.getJSONObject(j).getJSONObject("testResult").getString("status");

                                    if (status.equals("PASS"))
                                        monitorErrors.get(monitorErrors.indexOf(monitorError)).setTestedPASS(true);

                                }
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }

                        }

                    }

                }
            }
        }

    }

    public void setMonitorErrorCondition(List<MonitorError> monitorErrors) {
        for(MonitorError monitorError : monitorErrors) {
            monitorError.setIndex(monitorErrors.indexOf(monitorError));
            serviceRepository.setMonitorErrorConditionByAppId(monitorError.getErrorAppId(), "TRUE");
        }
    }


    public List<MonitorError> analyzeError(JSONArray array, String systemName, String serviceAppName, String serviceVersion) {

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
            boolean checkSwagger = false;

            /* A-B-C 都正常 但是回傳的時候B處理不好
             * 最根源的節點出錯是如何
             * 呼叫就呼叫不了
             * */
            boolean checkResponseError = false; // A-B-C 都正常 但是回傳的時候B處理不好
            boolean checkNodeError = false; // 確認目前分析的服務是否有出錯且是最後出錯的服務
            boolean checkRequestError = false; // 呼叫就呼叫不了

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
                continue;


            // 確認是否為ResponseError (Server & shared=true) Consumer沒有statusCode
            for(int j = 0; j < array_everyError.length(); j++){
                if(array_everyError.getJSONObject(j).getString("kind").equals("SERVER")){
                    if(array_everyError.getJSONObject(j).has("shared")) {
                        JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");
                        String appName = jsonObject.getString("http.appName");
                        String version = jsonObject.getString("http.version");
                        String id = array_everyError.getJSONObject(j).getString("id");

                        if (appName.toUpperCase().equals(serviceAppName) && version.toUpperCase().equals(serviceVersion)) {
                            if (jsonObject.has("error")) {
                                for(int k = 0; k < array_everyError.length(); k++){
                                    if(array_everyError.getJSONObject(k).getString("kind").equals("SERVER")){
                                        if(array_everyError.getJSONObject(k).has("parentId") && array_everyError.getJSONObject(k).getString("parentId").equals(id)){
                                            if(!array_everyError.getJSONObject(k).getJSONObject("tags").has("http.status_code")){
                                                checkResponseError = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 確認是否為NodeError
            for(int j = 0; j < array_everyError.length(); j++){
                if(array_everyError.getJSONObject(j).getString("kind").equals("SERVER")){
                    JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");
                    String appName = jsonObject.getString("http.appName");
                    String version = jsonObject.getString("http.version");
                    String id = array_everyError.getJSONObject(j).getString("id");

                    if(appName.toUpperCase().equals(serviceAppName) && version.toUpperCase().equals(serviceVersion)){
                        if (jsonObject.has("error")) { // 目前檢查的服務有錯
                            checkNodeError = true;
                            for(int k = 0; k < array_everyError.length(); k++){ // 目前檢查的服務是否為最後出錯的節點
                                if(array_everyError.getJSONObject(k).getString("kind").equals("SERVER")){
                                    if(array_everyError.getJSONObject(k).has("parentId") && array_everyError.getJSONObject(k).getString("parentId").equals(id)){
                                        checkNodeError = false;
                                    }
                                }
                            }
                        }
                    }

                }
            }

            // 確認是否為RequestError
            for(int j = 0; j < array_everyError.length(); j++) {
                if (array_everyError.getJSONObject(j).getString("kind").equals("SERVER")) {
                    JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");
                    String appName = jsonObject.getString("http.appName");
                    String version = jsonObject.getString("http.version");

                    if(appName.toUpperCase().equals(serviceAppName) && version.toUpperCase().equals(serviceVersion)){
                        if (jsonObject.has("error")) {
                            if(jsonObject.has("mvc.controller.method") && jsonObject.getString("mvc.controller.method").equals("errorHtml")){
                                checkRequestError = true;
                            }
                        }
                    }
                }
            }

            System.out.println("checkResponseError: " + checkResponseError);
            System.out.println("checkNodeError: " + checkNodeError);
            System.out.println("checkRequestError: " + checkRequestError);


            if(!checkResponseError && !checkNodeError && !checkRequestError)
                continue;


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


                    ErrorService errorService = new ErrorService(serviceId, appName, version, appId);
                    ErrorEndpoint errorEndpoint = new ErrorEndpoint(endpointId, appId, appName, endpointPath);
                    ErrorLink errorLink = new ErrorLink(linkId, serviceId, "OWN", endpointId);

                    if(appName.toUpperCase().equals(serviceAppName.toUpperCase()) && version.toUpperCase().equals(serviceVersion)){
                        errorService.setSourceOfError(true);
                        errorEndpoint.setSourceOfError(true);
                        errorLink.setSourceOfError(true);
                    }


                    es.add(errorService);
                    ee.add(errorEndpoint);
                    el.add(errorLink);

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

            // errorAppName, errorAppVersion, errorMessage, statusCode, timestamp, errorPath, consumerAppName...
            if(checkResponseError){
                for (int j = 0; j < array_everyError.length(); j++) {
                    if(array_everyError.getJSONObject(j).getString("kind").equals("SERVER")){
                        if (array_everyError.getJSONObject(j).has("shared") && array_everyError.getJSONObject(j).getBoolean("shared")) {
                            JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");
                            String serverId = array_everyError.getJSONObject(j).getString("id");

                            String appName = jsonObject.getString("http.appName");
                            String version = jsonObject.getString("http.version");

                            if (appName.toUpperCase().equals(serviceAppName) && version.toUpperCase().equals(serviceVersion)) {
                                if (jsonObject.has("error")) {
                                    errorAppName = jsonObject.getString("http.appName");
                                    errorAppVersion = jsonObject.getString("http.version");

                                    if (!errorAppName.toUpperCase().equals(serviceAppName) || !errorAppVersion.toUpperCase().equals(serviceVersion))
                                        continue;

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
                }
            } else if(checkNodeError) {
                for (int j = 0; j < array_everyError.length(); j++) {
                    if (array_everyError.getJSONObject(j).has("shared") && array_everyError.getJSONObject(j).getBoolean("shared")) {
                        JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");
                        String serverId = array_everyError.getJSONObject(j).getString("id");

                        String appName = jsonObject.getString("http.appName");
                        String version = jsonObject.getString("http.version");

                        if (appName.toUpperCase().equals(serviceAppName) && version.toUpperCase().equals(serviceVersion)) {
                            if (jsonObject.has("error")) {
                                errorAppName = jsonObject.getString("http.appName");
                                errorAppVersion = jsonObject.getString("http.version");

                                if (!errorAppName.toUpperCase().equals(serviceAppName) || !errorAppVersion.toUpperCase().equals(serviceVersion))
                                    continue;

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
            } else if(checkRequestError){
                for (int j = 0; j < array_everyError.length(); j++) {
                    if(array_everyError.getJSONObject(j).getString("kind").equals("SERVER")){
                        JSONObject jsonObject = array_everyError.getJSONObject(j).getJSONObject("tags");
                        String appName = jsonObject.getString("http.appName");
                        String version = jsonObject.getString("http.version");
                        String id = array_everyError.getJSONObject(j).getString("id");

                        if (appName.toUpperCase().equals(serviceAppName) && version.toUpperCase().equals(serviceVersion)) {
                            if (jsonObject.has("error")) {
                                errorAppName = jsonObject.getString("http.appName");
                                errorAppVersion = jsonObject.getString("http.version");

                                if (!errorAppName.toUpperCase().equals(serviceAppName) || !errorAppVersion.toUpperCase().equals(serviceVersion))
                                    continue;

                                errorMessage = jsonObject.getString("error");
                                errorMethod = jsonObject.getString("http.method");
                                errorPath = jsonObject.getString("http.path");
                                errorUrl = jsonObject.getString("http.url");
                                statusCode = jsonObject.getString("http.status_code");
                                timestamp = array_everyError.getJSONObject(j).getLong("timestamp");
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
            monitorError.setDate(dateFormat2.format(timestamp / 1000));
            monitorError.setErrorPath(errorPath);
            monitorError.setErrorUrl(errorUrl);
            monitorError.setErrorMethod(errorMethod);

            /*boolean checkResponseError = false; // A-B-C 都正常 但是回傳的時候B處理不好
            boolean checkNodeError = false; // 確認目前分析的服務是否有出錯且是最後出錯的服務
            boolean checkRequestError = false; // 呼叫就呼叫不了*/

            if(checkResponseError)
                monitorError.setErrorType("ResponseError");
            if(checkNodeError)
                monitorError.setErrorType("NodeError");
            if(checkRequestError)
                monitorError.setErrorType("RequestError");


            // 刪除重複
            Set<ErrorService> setEs = new HashSet<>(es);
            Set<ErrorEndpoint> setEe = new HashSet<>(ee);
            Set<ErrorLink> setEl = new HashSet<>(el);
            ArrayList<ErrorService> newEs = new ArrayList<>(setEs);
            ArrayList<ErrorEndpoint> newEe = new ArrayList<>(setEe);
            ArrayList<ErrorLink> newEl = new ArrayList<>(setEl);

            monitorError.setErrorServices(newEs);
            monitorError.setErrorEndpoints(newEe);
            monitorError.setErrorLinks(newEl);


            monitorErrorList.add(monitorError);


        }


        return monitorErrorList;
    }

    private List<MonitorError> pushMonitorError(List<MonitorError> monitorErrors, List<MonitorError> monitorErrors2) {

        for(MonitorError monitorError : monitorErrors) {
            monitorError.setIndex(0);
        }

        if(!monitorErrors2.isEmpty()) {
            // 刪除重複
            for(int i = monitorErrors.size() - 1; i >= 0; i--){
                MonitorError monitorError = monitorErrors.get(i);

                for(int j = 0; j < monitorErrors2.size(); j++){
                    MonitorError monitorError2 = monitorErrors2.get(j);
                    if(monitorError.getTimestamp() == monitorError2.getTimestamp() &&
                            monitorError.getErrorUrl().equals(monitorError2.getErrorUrl()) &&
                            monitorError.getErrorType().equals(monitorError2.getErrorType()) &&
                            monitorError.getErrorAppId().equals(monitorError2.getErrorAppId())){
                        monitorErrors.remove(monitorErrors.indexOf(monitorError));
                    }
                }
            }

            monitorErrors.addAll(0, monitorErrors2);
        }

        return monitorErrors;
    }

    public List<MonitorError> getErrorsOfSystem(String systemName) {
        return allMonitorErrorList.getOrDefault(systemName, new ArrayList<>());
    }

    public List<MonitorError> getSimulateErrorsOfSystem(String systemName) {
        return allSimulateMonitorErrorList.getOrDefault(systemName, new ArrayList<>());
    }

    public TotalErrorChart getErrorChart(String systemName) {
        TotalErrorChart totalErrorChart = new TotalErrorChart();

        Map<String,Integer> map = new HashMap<>();

        int totalDay = 140;
        int timeInterval = 20;

        // 真實系統用的方法
        List<MonitorError> monitorErrorList = allMonitorErrorList.getOrDefault(systemName, new ArrayList<>());

        // 模擬錯誤用的方法
        //List<MonitorError> monitorErrorList = allSimulateMonitorErrorList.getOrDefault(systemName, new ArrayList<>());
        long nowTime = System.currentTimeMillis();

        for ( int i = 0; i < totalDay; i+= timeInterval) {
            try {
                int countError = 0;
                String str1 = dateFormat3.format(nowTime - i * 24 * 60 * 60 * 1000L);
                Date date1 = dateFormat3.parse(str1);
                String str2 = dateFormat3.format(nowTime - (i + timeInterval) * 24 * 60 * 60 * 1000L);
                Date date2 = dateFormat3.parse(str2);

                for(int j = 0; j < monitorErrorList.size(); j++ ) {
                    long monitorErrorTimestamp = monitorErrorList.get(j).getTimestamp() / 1000L;

                    String str3 = dateFormat3.format(monitorErrorTimestamp);
                    Date date3 = dateFormat3.parse(str3);

                    Calendar cal1 = Calendar.getInstance();
                    Calendar cal2 = Calendar.getInstance();
                    Calendar cal3 = Calendar.getInstance();
                    cal1.setTime(date1);
                    cal2.setTime(date2);
                    cal3.setTime(date3);

                    if (cal3.before(cal1) && cal3.after(cal2)) {
                        countError++;
                    }
                }

                map.put(str1, countError);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }


        totalErrorChart.setMap(map);

        return totalErrorChart;
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
