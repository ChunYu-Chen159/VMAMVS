package com.soselab.microservicegraphplatform.bean.mgp.monitor.chart;

import java.util.HashMap;
import java.util.Map;

public class RiskPositivelyCorrelatedChart {
    Map<String,Double> servicesErrorNum = new HashMap<>();
    Map<String,Double> risk = new HashMap<>();


    public Map<String, Double> getServicesErrorNum() {
        return servicesErrorNum;
    }

    public void setServicesErrorNum(Map<String, Double> servicesErrorNum) {
        this.servicesErrorNum = servicesErrorNum;
    }

    public Map<String, Double> getRisk() {
        return risk;
    }

    public void setRisk(Map<String, Double> risk) {
        this.risk = risk;
    }
}
