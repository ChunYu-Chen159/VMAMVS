package com.soselab.microservicegraphplatform.services;

import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import com.soselab.microservicegraphplatform.repositories.neo4j.GeneralRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class RiskService {

    @Autowired
    private SleuthService sleuthService;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private GeneralRepository generalRepository;

    private final int totalDay = 180; // 總天數180天
    private final int timeInterval = 6; // 6天為間隔

    private final int STATUSCODE500 = 500;
    private final int STATUSCODE502 = 502;
    private final int STATUSCODE503 = 503;
    private final int STATUSCODE504 = 504;


    public void setServiceRisk(String systemName) {
        Long nowTime = System.currentTimeMillis();
        Long lookback = timeInterval * 24 * 60 * 60 * 1000L;
        int limit = 10000;

        List<Service> ServicesInDB = serviceRepository.findBySysName(systemName);

        Map<String,Object> averageMap = new HashMap<>();

        // 計算所有服務的平均值，totalDay時間內，以timeInterval為間隔找出各間隔服務錯誤數，並找出最大最小值算出平均值
        for(Service s : ServicesInDB) {
            Long endTime = nowTime;
            ArrayList<Integer> al = new ArrayList<Integer>();

            for ( int i = 0; i < totalDay; i+=timeInterval) {

                String jsonContent_500 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE500, lookback, endTime, limit);
                String jsonContent_502 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE502, lookback, endTime, limit);
                String jsonContent_503 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE503, lookback, endTime, limit);
                String jsonContent_504 = sleuthService.searchZipkin(s.getAppName(), s.getVersion(), STATUSCODE504, lookback, endTime, limit);

                int totalnum_500 = sleuthService.getTotalNum(jsonContent_500);
                int totalnum_502 = sleuthService.getTotalNum(jsonContent_502);
                int totalnum_503 = sleuthService.getTotalNum(jsonContent_503);
                int totalnum_504 = sleuthService.getTotalNum(jsonContent_504);

                al.add(totalnum_500 + totalnum_502 + totalnum_503 + totalnum_504);

                endTime -= lookback;

            }

            Object max = Collections.max(al);
            Object min = Collections.min(al);
            double average = ((int)max + (int)min) / 2.0;


            averageMap.put(s.getAppId(),average);

        }

        // 找出服務影響到的端點數量
        Map<String,Object> endpointNumberMap = new HashMap<>();
        for(Service s : ServicesInDB) {

            String provider = generalRepository.getProviders(s.getId());

            JSONObject jsonObj = new JSONObject(provider);
            JSONArray nodes = jsonObj.getJSONArray("nodes");

            double totalNum = 0;
            for(int j = 0; j < nodes.length(); j++) {
                totalNum += getNumofEndpoint((int)nodes.getJSONObject(j).get("id"));
            }

            System.out.println(s.getAppId() + " totalNum:");
            System.out.println(totalNum);


            endpointNumberMap.put(s.getAppId(), totalNum);

        }


        // 正規化[0.1, 1]
        Map<String,Object> likelihoodMap = new HashMap<>();
        likelihoodMap = normalization(averageMap, ServicesInDB);

        // 正規化[0.1, 1]
        Map<String,Object> impactMap = new HashMap<>();
        impactMap = normalization(endpointNumberMap, ServicesInDB);


        // 計算RiskValue，放到neo4j存
        for(Service s : ServicesInDB) {
            System.out.println(s.getAppId());
            System.out.println((double)likelihoodMap.get(s.getAppId()));
            System.out.println((double)impactMap.get(s.getAppId()));
            System.out.println((double)likelihoodMap.get(s.getAppId()) * (double)impactMap.get(s.getAppId()));

            double riskValue = (double)likelihoodMap.get(s.getAppId()) * (double)impactMap.get(s.getAppId());
            serviceRepository.setRiskValueByAppId(s.getAppId(), riskValue);
        }


    }


    public double getNumofEndpoint(long id) {

        String provider = generalRepository.getProviders(id);
        JSONObject jsonObj = new JSONObject(provider);
        JSONArray nodes = jsonObj.getJSONArray("nodes");

        if(nodes.length() > 0){
            double totalNum = 0;
            for(int j = 0; j < nodes.length(); j++) {
                totalNum += getNumofEndpoint((int)nodes.getJSONObject(j).get("id"));
                System.out.println("aaaaaaaa:" + totalNum);
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
