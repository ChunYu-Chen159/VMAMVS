package com.soselab.microservicegraphplatform.services;

import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.error.NodeError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.error.RequestError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.error.ResponseError;
import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRepository;
import jdk.vm.ci.services.Services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.*;

@Configuration
public class MonitorErrorSimulator {

    private final int totalDay = 210; // 總天數210天，當天到前30天計算各服務錯誤總數量，前31~210天計算各服務風險值
    private final int hours = 24; // 一天24小時

    @Autowired
    private ServiceRepository serviceRepository;

    Random random = new Random();
    final int minErrorNum = 150;
    final int maxErrorNum = 450;

    final int timeInterval = 180;

    private static final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");


    public MonitorErrorSimulator(){}




    // 視覺化 https://noob.tw/web-visualization-chartjs/
    // https://www.ucamc.com/e-learning/javascript/270-%E7%B0%A1%E5%96%AE%E4%BD%BF%E7%94%A8chart-js%E7%B6%B2%E9%A0%81%E4%B8%8A%E7%95%AB%E5%9C%96%E8%A1%A8%E7%AF%84%E4%BE%8B%E9%9B%86-javascript-%E5%9C%96%E8%A1%A8%E3%80%81jquery%E5%9C%96%E8%A1%A8%E7%B9%AA%E8%A3%BD

    public  List<MonitorError> simulateErrors(String systemName){
        List<Service> ServicesInDB = serviceRepository.findBySysName(systemName);

        for(Service s : ServicesInDB){
            if(s.getAppName().toUpperCase().equals("CINEMACATALOG") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                s.setErrorProbability(0.01);
            }else if(s.getAppName().toUpperCase().equals("GROCERYINVENTORY") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                s.setErrorProbability(0.00);
            }else if(s.getAppName().toUpperCase().equals("ORDERING") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                s.setErrorProbability(0.04);
            }else if(s.getAppName().toUpperCase().equals("PAYMENT") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                s.setErrorProbability(0.01);
            }else if(s.getAppName().toUpperCase().equals("NOTIFICATION") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                s.setErrorProbability(0.00);
            }
        }


        List<MonitorError> monitorErrors = new ArrayList<>();

        long nowTime = System.currentTimeMillis();
//        int randomErrorNum = random.nextInt((maxErrorNum - minErrorNum) + 1) + minErrorNum;

        for(int i = 0; i < totalDay; i++) {
            for(int j = 0; j < hours; j++) {
                for(Service s : ServicesInDB){
                    if(rateRandom(s.getErrorProbability())){
                        long errorTimestamp = nowTime - i * 24 * 60 * 60 * 1000L - j * 60 * 60 * 1000L;
                        if(s.getAppName().toUpperCase().equals("CINEMACATALOG") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                            MonitorError monitorError = new RequestError();
                            monitorError.setTimestamp(errorTimestamp * 1000L);
                            monitorError.setDate(dateFormat2.format(errorTimestamp));
                            monitorErrors.add(monitorError);
                        }else if(s.getAppName().toUpperCase().equals("GROCERYINVENTORY") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                        }else if(s.getAppName().toUpperCase().equals("ORDERING") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                            MonitorError monitorError = new ResponseError();
                            monitorError.setTimestamp(errorTimestamp * 1000L);
                            monitorError.setDate(dateFormat2.format(errorTimestamp));
                            monitorErrors.add(monitorError);
                        }else if(s.getAppName().toUpperCase().equals("PAYMENT") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                            MonitorError monitorError = new NodeError();
                            monitorError.setTimestamp(errorTimestamp * 1000L);
                            monitorError.setDate(dateFormat2.format(errorTimestamp));
                            monitorErrors.add(monitorError);
                        }else if(s.getAppName().toUpperCase().equals("NOTIFICATION") && s.getVersion().equals("0.0.1-SNAPSHOT")){
                        }
                    }
                }
            }
        }
/*

        for( int i = 0; i < randomErrorNum; i++) {

            int randomTime = random.nextInt(timeInterval);

            long errorTimestamp = nowTime - randomTime * 24 * 60 * 60 * 1000L;

            int randomError = random.nextInt(3) + 1;

            if(randomError == 1){
                MonitorError monitorError = new ResponseError();
                monitorError.setTimestamp(errorTimestamp * 1000L);
                monitorError.setDate(dateFormat2.format(errorTimestamp));
                monitorErrors.add(monitorError);
            }else if(randomError == 2){
                MonitorError monitorError = new NodeError();
                monitorError.setTimestamp(errorTimestamp * 1000L);
                monitorError.setDate(dateFormat2.format(errorTimestamp));
                monitorErrors.add(monitorError);
            }else if(randomError == 3) {
                MonitorError monitorError = new RequestError();
                monitorError.setTimestamp(errorTimestamp * 1000L);
                monitorError.setDate(dateFormat2.format(errorTimestamp));
                monitorErrors.add(monitorError);
            }


        }*/
        return monitorErrors;
    }


    public boolean rateRandom(double rate){
        Random r = new Random();
        int num = r.nextInt(10000) + 1;

        if(num <= (int)(rate * 1000)){
            return true;
        }else{
            return false;
        }
    }
}
