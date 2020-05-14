package com.soselab.microservicegraphplatform.bean.actuators;

public class Info {

    private String version;

    public Info() {
    }

    public Info(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
