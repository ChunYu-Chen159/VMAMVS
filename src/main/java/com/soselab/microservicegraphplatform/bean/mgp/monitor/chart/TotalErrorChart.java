package com.soselab.microservicegraphplatform.bean.mgp.monitor.chart;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TotalErrorChart {

    Map<String,Integer> map = new HashMap<>();


    public Map<String, Integer> getMap() {
        return map;
    }

    public void setMap(Map<String, Integer> map) {
        this.map = map;
    }
}
