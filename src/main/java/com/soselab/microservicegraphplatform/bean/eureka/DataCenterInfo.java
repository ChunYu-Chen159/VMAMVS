package com.soselab.microservicegraphplatform.bean.eureka;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataCenterInfo {

    private String infoClass;
    private String name;

    public DataCenterInfo() {}

    public DataCenterInfo(String infoClass, String name) {
        this.infoClass = infoClass;
        this.name = name;
    }

    @JsonProperty("@class")
    public String getInfoClass() {
        return infoClass;
    }

    @JsonProperty("@class")
    public void setInfoClass(String infoClass) {
        this.infoClass = infoClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
