package com.soselab.microservicegraphplatform.bean.mgp.monitor.chart;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TotalErrorChart {

    Map<Date,Integer> map = new HashMap<>();


    public Map<Date, Integer> getMap() {
        return map;
    }

    public void setMap(Map<Date, Integer> map) {
        this.map = map;
    }
}
