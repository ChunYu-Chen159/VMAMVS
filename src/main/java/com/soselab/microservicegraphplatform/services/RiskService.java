package com.soselab.microservicegraphplatform.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiskService {

    @Autowired
    private SleuthService sleuthService;

    private final int totalDay = 180; // 總天數180天
    private final int timeInterval = 6; // 6天為間隔


    public void risk() {
        Long nowTime = System.currentTimeMillis();


        Long startTime = nowTime;
        Long nextTime = startTime - (timeInterval * 24 * 60 * 60 * 1000L);



        startTime = nextTime;

    }


}
