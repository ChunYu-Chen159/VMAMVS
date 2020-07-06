package com.soselab.microservicegraphplatform.services;

import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;
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

    private final int totalDay = 180; // 總天數180天
    private final int timeInterval = 6; // 6天為間隔
    private final int moveInterval = 1; // 每次移動的距離

    private final int STATUSCODE500 = 500;
    private final int STATUSCODE502 = 502;
    private final int STATUSCODE503 = 503;
    private final int STATUSCODE504 = 504;


    public void setServiceRisk(String systemName) {
        long nowTime = System.currentTimeMillis();
        long lookback = timeInterval * 24 * 60 * 60 * 1000L;
        long move = moveInterval * 24 * 60 * 60 * 1000L;
        int limit = 10000;

        List<Service> ServicesInDB = serviceRepository.findBySysName(systemName);

        Map<String,Object> averageMap = new HashMap<>();

        List<MonitorError> simulatorMonitorErrors = monitorService.getSimulateErrorsOfSystem(systemName);

        // 計算所有服務的平均值，totalDay時間內，以timeInterval為間隔找出各間隔服務錯誤數，並找出最大最小值算出平均值
        for(Service s : ServicesInDB) {
            Long endTime = nowTime;
            ArrayList<Integer> al = new ArrayList<Integer>();

            for ( int i = 0; i < totalDay - timeInterval + 1; i++) {

                // 分析真實錯誤用的方法
                /*String jsonContent_500 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE500, lookback, endTime, limit);
                String jsonContent_502 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE502, lookback, endTime, limit);
                String jsonContent_503 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE503, lookback, endTime, limit);
                String jsonContent_504 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE504, lookback, endTime, limit);

                int totalnum_500 = sleuthService.getTotalNum(jsonContent_500);
                int totalnum_502 = sleuthService.getTotalNum(jsonContent_502);
                int totalnum_503 = sleuthService.getTotalNum(jsonContent_503);
                int totalnum_504 = sleuthService.getTotalNum(jsonContent_504);*/

                // 分析模擬錯誤用的方法
                int totalNum = 0;
                for(int j = 0; j < simulatorMonitorErrors.size(); j++){
                    MonitorError monitorError = simulatorMonitorErrors.get(j);
                    if(s.getAppId().equals(monitorError.getErrorAppId())) {
                        try {

                            String str1 = dateFormat2.format(endTime);
                            Date date1 = dateFormat2.parse(str1);
                            String str2 = dateFormat2.format(endTime - lookback);
                            Date date2 = dateFormat2.parse(str2);
                            String str3 = dateFormat2.format(monitorError.getTimestamp() / 1000L);
                            Date date3 = dateFormat2.parse(str3);

                            Calendar cal1 = Calendar.getInstance();
                            Calendar cal2 = Calendar.getInstance();
                            Calendar cal3 = Calendar.getInstance();
                            cal1.setTime(date1);
                            cal2.setTime(date2);
                            cal3.setTime(date3);

                            if (cal3.before(cal1) && cal3.after(cal2)) {
                                totalNum++;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                }
                al.add(totalNum);
                // 分析真實錯誤用的方法
//                al.add(totalnum_500 + totalnum_502 + totalnum_503 + totalnum_504);

                endTime -= move;

            }

            System.out.println("Service: " + s.getAppId());

            Object highStandard = Collections.max(al);
            Object lowStandard = Collections.min(al);
            double average = ((int)highStandard + (int)lowStandard) / 2.0;

            System.out.println("highStandard: " + highStandard);
            System.out.println("lowStandard: " + lowStandard);
            System.out.println("average: " + average);

            averageMap.put(s.getAppId(),average);

        }

        // 找出服務影響到的端點數量
        Map<String,Object> endpointNumberMap = new HashMap<>();
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

            System.out.println("Service: " + s.getAppId());
            System.out.println("impactTotalNum: " + totalNum);

            endpointNumberMap.put(s.getAppId(), totalNum);

        }

        // 正規化[0.1, 1]
        Map<String,Object> likelihoodMap = new HashMap<>();
        System.out.println("likelihoodMap:" );
        likelihoodMap = normalization(averageMap, ServicesInDB);

        // 正規化[0.1, 1]
        Map<String,Object> impactMap = new HashMap<>();
        System.out.println("impactMap:" );
        impactMap = normalization(endpointNumberMap, ServicesInDB);

        // 計算RiskValue，放到neo4j存
        for(Service s : ServicesInDB) {

            System.out.println("Service: " + s.getAppId());
            System.out.println("likelihoodMap.get(s.getAppId()): " + likelihoodMap.get(s.getAppId()));
            System.out.println("impactMap.get(s.getAppId()): " + impactMap.get(s.getAppId()));

            double riskValue = (double)likelihoodMap.get(s.getAppId()) * (double)impactMap.get(s.getAppId());
            serviceRepository.setRiskValueByAppId(s.getAppId(), riskValue);
        }


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
    public Map<String,Object> normalization(Map<String,Object> map, List<Service> ServicesInDB){
        double a = 0.1;
        double b = 1;

        Map<String,Object> returnMap = new HashMap<>();

        if (map != null) {
            double max = (double)map.get(ServicesInDB.get(0).getAppId());
            double min = (double)map.get(ServicesInDB.get(0).getAppId());
            double k;

            // 找max , min
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if(max < (double)value)
                    max = (double)value;
                if(min > (double)value)
                    min = (double)value;
            }

            // 計算係數k
            k = (b-a)/(max-min);
            System.out.println("max:"  + max);
            System.out.println("min:"  + min);
            System.out.println("k:"  + k);

            // 套入公式正規化
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                double NorY = a + k * ((double)value - min);

                returnMap.put(key, NorY);

            }
        }

        return returnMap;
    }


}
