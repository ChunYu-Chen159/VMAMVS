package com.soselab.microservicegraphplatform.bean.mgp.monitor;


import java.util.ArrayList;

public class MonitorError {

    // timestamp使用 https://codertw.com/%E7%A8%8B%E5%BC%8F%E8%AA%9E%E8%A8%80/319101/
    private String errorAppName;
    private long timestamp;
    private String statusCode;
    private String errorMessage;

    private ArrayList<ErrorService> es;
    private ArrayList<ErrorEndpoint> ee;
    private ArrayList<ErrorLink> el;

    public String getErrorAppName() {
        return errorAppName;
    }

    public void setErrorAppName(String errorAppName) {
        this.errorAppName = errorAppName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ArrayList<ErrorService> getEs() {
        return es;
    }

    public void setEs(ArrayList<ErrorService> es) {
        this.es = es;
    }

    public ArrayList<ErrorEndpoint> getEe() {
        return ee;
    }

    public void setEe(ArrayList<ErrorEndpoint> ee) {
        this.ee = ee;
    }

    public ArrayList<ErrorLink> getEl() {
        return el;
    }

    public void setEl(ArrayList<ErrorLink> el) {
        this.el = el;
    }
}
