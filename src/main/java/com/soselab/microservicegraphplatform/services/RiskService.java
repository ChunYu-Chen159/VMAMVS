package com.soselab.microservicegraphplatform.services;

import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.chart.RiskPositivelyCorrelatedChart;
import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import com.soselab.microservicegraphplatform.repositories.neo4j.GeneralRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Configuration
public class RiskService {

    private static final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Autowired
    private SleuthService sleuthService;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private GeneralRepository generalRepository;
    @Autowired
    private MonitorService monitorService;

    private final int totalDay = 84; // 總天數84天
    private final int timeInterval = 21; // 21天為間隔
    private final int moveInterval = 1; // 每次移動的距離

    private final int beginTime1 = 8; // 第2周開始
    private final int endTime1 = 28; // 到第4周
    private final int beginTime2 = 29; // 第5周開始
    private final int endTime2 = 84; // 到第12周

    private final int STATUSCODE500 = 500;
    private final int STATUSCODE502 = 502;
    private final int STATUSCODE503 = 503;
    private final int STATUSCODE504 = 504;

    Map<String,Double> thisWeekErrorNumMap = new HashMap<>();


    public void setServiceRisk(String systemName) {
        long nowTime = System.currentTimeMillis();
        long lookback = timeInterval * 24 * 60 * 60 * 1000L; // 實際使用天數為單位
//        long lookback = timeInterval * 60 * 1000L; // 模擬錯誤用分鐘為單位

        // 用來計算第一周的衍生錯誤
        long lookback_thisWeek = 7 * 24 * 60 * 60 * 1000L; // 實際使用天數為單位
//        long lookback_thisWeek = 7 * 60 * 1000L; // 模擬錯誤用分鐘為單位

        long move = moveInterval * 24 * 60 * 60 * 1000L; // 實際使用天數為單位
//        long move = moveInterval * 60 * 1000L; // 模擬用分鐘為單位
        int limit = 10000;

        List<Service> ServicesInDB = serviceRepository.findBySysName(systemName);

        Map<String,Object> averageMap = new HashMap<>();

        //List<MonitorError> simulatorMonitorErrors = monitorService.getSimulateErrorsOfSystem(systemName);

        // Likelihood，
        // 第5周~12周(8周) ==> 算高標(ex:8.5)、低標(ex:1.1)，
        // 第2周~第4周(3周) ==> 找各服務所有的錯誤數，算風險值 (根據高低標縮放比例，縮放至1~0.1)

        // 第5周~12周(8周) ==> 算高標(ex:8.5)、低標(ex:1.1)，
        ArrayList<Integer> highlowStandards_errors = new ArrayList<>();
        double highStandard = 0.0;
        double lowStandard = 0.0;
        for(Service s : ServicesInDB) {
            Long endTime = nowTime + beginTime2 * 24 * 60 * 60 * 1000L; // 實際用天數為單位
//            Long endTime = nowTime - beginTime2 * 60 * 1000L; // 模擬用分鐘數為單位

            for ( int i = 0; i < endTime2 - beginTime2 + 1; i++) {
                String jsonContent_500 = "[]";
                String jsonContent_502 = "[]";
                String jsonContent_503 = "[]";
                String jsonContent_504 = "[]";

                try {
                    jsonContent_500 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE500, lookback, endTime, limit);
                    jsonContent_502 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE502, lookback, endTime, limit);
                    jsonContent_503 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE503, lookback, endTime, limit);
                    jsonContent_504 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE504, lookback, endTime, limit);
                }catch(NullPointerException e){
                    e.printStackTrace();
                }

                int totalnum_500 = sleuthService.getTotalNum(jsonContent_500);
                int totalnum_502 = sleuthService.getTotalNum(jsonContent_502);
                int totalnum_503 = sleuthService.getTotalNum(jsonContent_503);
                int totalnum_504 = sleuthService.getTotalNum(jsonContent_504);

                highlowStandards_errors.add(totalnum_500 + totalnum_502 + totalnum_503 + totalnum_504);

                endTime -= move;
            }
        }
        Collections.sort(highlowStandards_errors);
        System.out.println("allllllllllllllllllllllll:"  + highlowStandards_errors);

        int highStandard_total = 0;
        double highStandard_count = 0.0;
        int lowStandard_total = 0;
        double lowStandard_count = 0.0;


        for(int i = 0; i < highlowStandards_errors.size()/4-1; i++){
            lowStandard_total += highlowStandards_errors.get(i);
            lowStandard_count++;
        }

        for(int i = highlowStandards_errors.size()-1; i > highlowStandards_errors.size()/4 * 3 + 1; i--) {
            highStandard_total += highlowStandards_errors.get(i);
            highStandard_count++;
        }

        highStandard = highStandard_total/highStandard_count;
        lowStandard = lowStandard_total/lowStandard_count;

        System.out.println("highStandard: " + highStandard);
        System.out.println("lowStandard: " + lowStandard);


        // 第2周~第4周(3周) ==> 找各服務所有的錯誤數，算風險值 (根據高低標縮放比例，縮放至1~0.1)
        Map<String,Double> servicesErrorNumMap = new HashMap<>();
        for(Service s : ServicesInDB) {
            Long endTime = nowTime + beginTime1 * 24 * 60 * 60 * 1000L; // 實際用天數為單位
//            Long endTime = nowTime - beginTime1 * 60 * 1000L; // 模擬用分鐘為單位
            double serviceErrors = 0.0;
            for ( int i = 0; i < endTime1 - beginTime1 + 1; i++) {
                String jsonContent_500 = "[]";
                String jsonContent_502 = "[]";
                String jsonContent_503 = "[]";
                String jsonContent_504 = "[]";

                try {
                    jsonContent_500 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE500, lookback, endTime, limit);
                    jsonContent_502 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE502, lookback, endTime, limit);
                    jsonContent_503 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE503, lookback, endTime, limit);
                    jsonContent_504 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE504, lookback, endTime, limit);
                }catch(NullPointerException e){
                    e.printStackTrace();
                }

                int totalnum_500 = sleuthService.getTotalNum(jsonContent_500);
                int totalnum_502 = sleuthService.getTotalNum(jsonContent_502);
                int totalnum_503 = sleuthService.getTotalNum(jsonContent_503);
                int totalnum_504 = sleuthService.getTotalNum(jsonContent_504);

                serviceErrors += totalnum_500 + totalnum_502 + totalnum_503 + totalnum_504;

                endTime -= move;
            }

            servicesErrorNumMap.put(s.getAppId(), serviceErrors);
        }

        System.out.println("\nservicesErrorNumMap: ");
        for (Map.Entry<String, Double> entry : servicesErrorNumMap.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();

            System.out.println(key + ": " + value);
        }

        // 找出服務影響到的端點數量
        Map<String,Double> endpointNumberMap = new HashMap<>();
        for(Service s : ServicesInDB) {

            String provider = generalRepository.getProviders(s.getId());
            String consumer = generalRepository.getConsumers(s.getId());

            JSONObject jsonObj = new JSONObject(provider);
            JSONArray nodes = jsonObj.getJSONArray("nodes");

            JSONObject jsonObj2 = new JSONObject(consumer);
            JSONArray nodes2 = jsonObj2.getJSONArray("nodes");

            double totalNum = 0;
            for(int j = 0; j < nodes.length(); j++) {
                totalNum += getNumofEndpoint_Provider((int)nodes.getJSONObject(j).get("id"));
            }

            for(int j = 0; j < nodes2.length(); j++) {
                totalNum += getNumofEndpoint_Consumer((int)nodes2.getJSONObject(j).get("id"));
            }

            endpointNumberMap.put(s.getAppId(), (totalNum+1));

        }

        System.out.println("\nendpointNumberMap: ");
        for (Map.Entry<String, Double> entry : endpointNumberMap.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();

            System.out.println(key + ": " + value);
        }

        // 正規化[0.1, 1]
        Map<String,Double> likelihoodMap = new HashMap<>();
//        likelihoodMap2 = normalization(averageMap, ServicesInDB);
        likelihoodMap = normalization_likelihood(servicesErrorNumMap, highStandard, lowStandard);

        // 正規化[0.1, 1]
        Map<String,Double> impactMap = new HashMap<>();
        impactMap = normalization_impact(endpointNumberMap, ServicesInDB);

        // 計算RiskValue，放到neo4j存
        System.out.println("\nRiskValue: ");
        for(Service s : ServicesInDB) {

            double riskValue = likelihoodMap.get(s.getAppId()) * impactMap.get(s.getAppId());
            serviceRepository.setRiskValueByAppId(s.getAppId(), riskValue);

            System.out.println("Service: " + s.getAppId());
            System.out.println("likelihoodMap: " + likelihoodMap.get(s.getAppId()));
            System.out.println("impactMap: " + impactMap.get(s.getAppId()));
            System.out.println("riskValue: " + riskValue);

        }


        // 第1周 ==> 找各服務所有的錯誤數，算衍生錯誤
        System.out.println("\nthisWeekErrorNum: ");
        for(Service s : ServicesInDB) {
            System.out.println(s.getAppId());
            Long endTime = nowTime;
            double serviceErrors = 0.0;

            String jsonContent_500 = "[]";
            String jsonContent_502 = "[]";
            String jsonContent_503 = "[]";
            String jsonContent_504 = "[]";

            try {
                jsonContent_500 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE500, lookback_thisWeek, endTime, limit);
                jsonContent_502 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE502, lookback_thisWeek, endTime, limit);
                jsonContent_503 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE503, lookback_thisWeek, endTime, limit);
                jsonContent_504 = sleuthService.searchZipkinV1(s.getAppName(), s.getVersion(), STATUSCODE504, lookback_thisWeek, endTime, limit);
            }catch(NullPointerException e){
                e.printStackTrace();
            }

            int totalnum_500 = sleuthService.getTotalNum(jsonContent_500);
            int totalnum_502 = sleuthService.getTotalNum(jsonContent_502);
            int totalnum_503 = sleuthService.getTotalNum(jsonContent_503);
            int totalnum_504 = sleuthService.getTotalNum(jsonContent_504);

            System.out.println("totalnum_500: " + totalnum_500);
            System.out.println("totalnum_502: " + totalnum_502);
            System.out.println("totalnum_503: " + totalnum_503);
            System.out.println("totalnum_504: " + totalnum_504);
            System.out.println("endpointNumberMap: " + endpointNumberMap.get(s.getAppId()));


            serviceErrors += (totalnum_500 + totalnum_502 + totalnum_503 + totalnum_504) * endpointNumberMap.get(s.getAppId()) + (totalnum_500 + totalnum_502 + totalnum_503 + totalnum_504);


            thisWeekErrorNumMap.put(s.getAppId(), serviceErrors);
        }

        System.out.println("\nthisWeekErrorNumMap: ");
        for (Map.Entry<String, Double> entry : thisWeekErrorNumMap.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();

            System.out.println(key + ": " + value);
        }



    }

    public RiskPositivelyCorrelatedChart getRiskPositivelyCorrelatedChart(String systemName){
        RiskPositivelyCorrelatedChart riskPositivelyCorrelatedChart = new RiskPositivelyCorrelatedChart();

        long nowTime = System.currentTimeMillis();

        Map<String,Integer> servicesErrorNum = new HashMap<>();
        Map<String,Double> risk = new HashMap<>();

        List<Service> ServicesInDB = serviceRepository.findBySysName(systemName);
        // 真實系統用的方法
        List<MonitorError> monitorErrors = monitorService.getErrorsOfSystem(systemName);

        // 模擬錯誤用的方法
        //List<MonitorError> monitorErrors = monitorService.getSimulateErrorsOfSystem(systemName);

        for(Service s : ServicesInDB) {
            risk.put(s.getAppId(), serviceRepository.getRiskValueByAppId(s.getAppId()));
        }

        riskPositivelyCorrelatedChart.setServicesErrorNum(thisWeekErrorNumMap);
        riskPositivelyCorrelatedChart.setRisk(risk);


        return riskPositivelyCorrelatedChart;
    }

    public double getNumofEndpoint_Provider(long id) {
        String provider = generalRepository.getProviders(id);
        JSONObject jsonObj = new JSONObject(provider);
        JSONArray nodes = jsonObj.getJSONArray("nodes");

        if(nodes.length() > 0){
            double totalNum = nodes.length();
            for(int j = 0; j < nodes.length(); j++) {
                totalNum += getNumofEndpoint_Provider((int)nodes.getJSONObject(j).get("id"));
            }

            return totalNum;
        }else{
            return 0;
        }
    }

    public double getNumofEndpoint_Consumer(long id) {

        String provider = generalRepository.getProviders(id);
        JSONObject jsonObj = new JSONObject(provider);
        JSONArray nodes = jsonObj.getJSONArray("nodes");

        if(nodes.length() > 0){
            double totalNum = nodes.length();
            for(int j = 0; j < nodes.length(); j++) {
                totalNum += getNumofEndpoint_Consumer((int)nodes.getJSONObject(j).get("id"));
            }

            return totalNum;
        }else{
            return 0;
        }
    }

    // 正規化[0.1, 1]
    public Map<String,Double> normalization_likelihood(Map<String,Double> map, double highStandard, double lowStandard){
        double a = 0.1;
        double b = 1;

        Map<String,Double> returnMap = new HashMap<>();

        System.out.println("\nnormalization_likelihood: ");
        if (map != null) {
            double max = highStandard;
            double min = lowStandard;
            double k = 0.0;

            // 計算係數k
            k = (b - a) / (max - min);
            System.out.println("max:" + max);
            System.out.println("min:" + min);
            System.out.println("k:" + k);

            // 套入公式正規化
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                String key = entry.getKey();
                double value = entry.getValue();

                double NorY = a + k * (value - min);

                if(NorY > 1.0) NorY = 1.0;
                if(NorY < 0.1) NorY = 0.1;

                returnMap.put(key, NorY);

                System.out.println(key + ": " + value + " --> " + NorY);
            }
        }

        return returnMap;
    }


    // 正規化[0.1, 1]
    public Map<String,Double> normalization_impact(Map<String,Double> map, List<Service> ServicesInDB){
        double a = 0.1;
        double b = 1;

        Map<String,Double> returnMap = new HashMap<>();

        System.out.println("\nnormalization_impact: ");
        if (map != null) {
            double max = map.get(ServicesInDB.get(0).getAppId());
            double min = map.get(ServicesInDB.get(0).getAppId());
            double k;

            // 找max , min
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                String key = entry.getKey();
                double value = entry.getValue();
                if(max < value)
                    max = value;
                if(min > value)
                    min = value;
            }

            // 計算係數k
            k = (b-a)/(max-min);
            System.out.println("max:"  + max);
            System.out.println("min:"  + min);
            System.out.println("k:"  + k);

            // 套入公式正規化
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                double NorY = a + k * ((double)value - min);

                returnMap.put(key, NorY);

                System.out.println(key + ": " + value + " --> " + NorY);
            }
        }

        return returnMap;
    }

}
