package com.soselab.microservicegraphplatform.services;

import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.error.LastNodeError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.error.NullError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.error.ReturnError;
import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.*;

@Configuration
public class MonitorErrorSimulator {

    private ReturnError returnError;
    private LastNodeError lastNodeError;
    private NullError nullError;

    private final int totalDay = 180; // 總天數180天

    Random random = new Random();
    final int minErrorNum = 150;
    final int maxErrorNum = 450;

    final int timeInterval = 180;

    private static final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");


    public MonitorErrorSimulator(){}


    // 錯誤總數量隨機(150 ~ 450)
    // 隨機選擇錯誤類型
    // 錯誤週期 (模擬錯誤) (180天內)
    // * 常態分佈出錯
    // * 週期性平均出錯
    // * 尖峰出錯 (模擬周末出錯)
    // * 隨機出錯

    // 視覺化 https://noob.tw/web-visualization-chartjs/
    // https://www.ucamc.com/e-learning/javascript/270-%E7%B0%A1%E5%96%AE%E4%BD%BF%E7%94%A8chart-js%E7%B6%B2%E9%A0%81%E4%B8%8A%E7%95%AB%E5%9C%96%E8%A1%A8%E7%AF%84%E4%BE%8B%E9%9B%86-javascript-%E5%9C%96%E8%A1%A8%E3%80%81jquery%E5%9C%96%E8%A1%A8%E7%B9%AA%E8%A3%BD

    public  List<MonitorError> simulateError(String systemName){
        List<MonitorError> monitorErrors = new ArrayList<>();

        long nowTime = System.currentTimeMillis();
        int randomErrorNum = random.nextInt((maxErrorNum - minErrorNum) + 1) + minErrorNum;


        for( int i = 0; i < randomErrorNum; i++) {
            MonitorError monitorError = new MonitorError();
            int randomError = random.nextInt(3) + 1;
            switch (randomError) {
                case 1:
                    monitorError = returnError;
                    break;
                case 2:
                    monitorError = lastNodeError;
                    break;
                case 3:
                    monitorError = nullError;
                    break;
                default:
                    System.out.println("None");
                    break;
            }

            int randomTime = random.nextInt(timeInterval);

            long errorTimestamp = nowTime - randomTime * 24 * 60 * 60 * 1000L;

            monitorError.setTimestamp(errorTimestamp);
            monitorError.setDate(dateFormat2.format(errorTimestamp / 1000));



        }
        return monitorErrors;
    }

}
