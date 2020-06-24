package com.soselab.microservicegraphplatform.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import com.soselab.microservicegraphplatform.repositories.neo4j.GeneralRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRegistryRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class ContractService {

    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    @Autowired
    private GeneralRepository generalRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private SpringRestTool springRestTool;
    @Autowired
    private ObjectMapper mapper;

    private static final String CONTRACTTESTINGCONDITION_PASS = "PASS";
    private static final String CONTRACTTESTINGCONDITION_WARNING = "WARNING";


    public void setAllServiceContractTestingCondition(String systemName){
        List<Service> ServicesInDB = serviceRepository.findBySysName(systemName);

        for(Service s : ServicesInDB) {

            String condition = CONTRACTTESTINGCONDITION_PASS;

            List<String> providerService = generalRepository.getAllHttpRequestServiceWithService(s.getAppId());

            if(providerService != null && !providerService.isEmpty()) {

                for (String str : providerService) {
                    try {
                        JSONObject jsonObj = new JSONObject(str);

                        Map<String, Object> swaggerMap = springRestTool.getSwaggerFromRemoteApp2(jsonObj.getString("systemName"), jsonObj.getString("appName"), jsonObj.getString("version"));

                        if (swaggerMap != null) {


/*                            JSONObject jsonObj2 = new JSONObject(swaggerMap.get("x-contract"));
                            JSONObject jsonObj3 = new JSONObject(jsonObj2.getJSONObject(s.getAppName().toLowerCase() + ".groovy"));



                            JSONArray jsonArr = jsonObj3.get;*/

                            Map<String, Object> contractsMap = mapper.convertValue(swaggerMap.get("x-contract"), new TypeReference<Map<String, Object>>() {});
                            Map<String, Object> groovyMap = mapper.convertValue(contractsMap.get(s.getAppName().toLowerCase() + ".groovy"), new TypeReference<Map<String, Object>>() {});
                            for (Map.Entry<String, Object> entry : groovyMap.entrySet()) {
                                String key = entry.getKey();
                                Object value = entry.getValue();

                                System.out.println("key: " + key);
                                System.out.println("value: " + value);


                                Map<String, Object> apiMap = mapper.convertValue(value, new TypeReference<Map<String, Object>>() {
                                });
                                Map<String, Object> testResultMap = mapper.convertValue(apiMap.get("testResult"), new TypeReference<Map<String, Object>>() {
                                });
                                String status = mapper.convertValue(testResultMap.get("status"), new TypeReference<String>() {
                                });

                                if (status.equals("FAIL")) {
                                    serviceRepository.setContractTestingConditionByAppId(s.getAppId(), CONTRACTTESTINGCONDITION_WARNING);
                                    condition = CONTRACTTESTINGCONDITION_WARNING;
                                }

                            }


                        }


                    } catch (JSONException err) {
                        err.printStackTrace();
                        logger.error(err.toString());
                    }
                }
            }

            if( condition.equals(CONTRACTTESTINGCONDITION_PASS))
                serviceRepository.setContractTestingConditionByAppId(s.getAppId(), CONTRACTTESTINGCONDITION_PASS);

        }
    }




}
