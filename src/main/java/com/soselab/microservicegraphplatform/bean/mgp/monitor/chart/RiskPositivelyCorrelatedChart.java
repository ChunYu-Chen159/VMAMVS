package com.soselab.microservicegraphplatform.bean.mgp.monitor.chart;

import java.util.HashMap;
import java.util.Map;

public class RiskPositivelyCorrelatedChart {
    Map<String,Integer> servicesErrorNum = new HashMap<>();
    Map<String,Double> risk = new HashMap<>();


    public Map<String, Integer> getServicesErrorNum() {
        return servicesErrorNum;
    }

    public void setServicesErrorNum(Map<String, Integer> servicesErrorNum) {
        this.servicesErrorNum = servicesErrorNum;
    }

    public Map<String, Double> getRisk() {
        return risk;
    }

    public void setRisk(Map<String, Double> risk) {
        this.risk = risk;
    }
}
