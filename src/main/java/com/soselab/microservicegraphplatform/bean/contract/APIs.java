package com.soselab.microservicegraphplatform.bean.contract;

import java.util.Map;

public class APIs {

    final static String method_GET = "GET";
    final static String method_POST = "POST";
    final static String method_PUT = "PUT";
    final static String method_DELETE = "DELETE";

    private String method;
    private String url;
    private Map<String,String> parameters;
    private boolean erring;
    private boolean doubleCheck;

    public APIs(){
        this.erring = false;
        this.doubleCheck = false;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public boolean isErring() {
        return erring;
    }

    public void setErring(boolean erring) {
        this.erring = erring;
    }

    public boolean isDoubleCheck() {
        return doubleCheck;
    }

    public void setDoubleCheck(boolean doubleCheck) {
        this.doubleCheck = doubleCheck;
    }
}
