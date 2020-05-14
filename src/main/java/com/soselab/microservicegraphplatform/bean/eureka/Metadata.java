package com.soselab.microservicegraphplatform.bean.eureka;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {

    String managementPort;

    public Metadata() {}

    public Metadata(String managementPort) {
        this.managementPort = managementPort;
    }

    @JsonProperty("management.port")
    public String getManagementPort() {
        return managementPort;
    }

    @JsonProperty("management.port")
    public void setManagementPort(String managementPort) {
        this.managementPort = managementPort;
    }
}
