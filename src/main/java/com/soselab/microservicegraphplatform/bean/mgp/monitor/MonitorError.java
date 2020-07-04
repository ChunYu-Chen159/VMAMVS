package com.soselab.microservicegraphplatform.bean.mgp.monitor;


import java.util.ArrayList;

public class MonitorError {

    // timestamp使用 https://codertw.com/%E7%A8%8B%E5%BC%8F%E8%AA%9E%E8%A8%80/319101/
    private long index;
    private String errorAppId;
    private String errorSystemName;
    private String errorAppName;
    private String errorAppVersion;
    private String consumerAppName;
    private long timestamp;
    private String date;
    private String statusCode;
    private String errorMessage;
    private String errorPath;
    private String errorUrl;
    private String errorMethod;
    private String errorType; // ReturnError, LastNodeError, NullError
    private boolean testedPASS;

    private ArrayList<ErrorService> errorServices;
    private ArrayList<ErrorEndpoint> errorEndpoints;
    private ArrayList<ErrorLink> errorLinks;

    public MonitorError() {
        this.index = 0;
        this.testedPASS = false;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public String getErrorAppId() {
        return errorAppId;
    }

    public void setErrorAppId(String errorAppId) {
        this.errorAppId = errorAppId;
    }

    public String getErrorSystemName() {
        return errorSystemName;
    }

    public void setErrorSystemName(String errorSystemName) {
        this.errorSystemName = errorSystemName;
    }

    public String getErrorAppName() {
        return errorAppName;
    }

    public void setErrorAppName(String errorAppName) {
        this.errorAppName = errorAppName;
    }

    public String getErrorAppVersion() {
        return errorAppVersion;
    }

    public void setErrorAppVersion(String errorAppVersion) {
        this.errorAppVersion = errorAppVersion;
    }

    public String getConsumerAppName() {
        return consumerAppName;
    }

    public void setConsumerAppName(String consumerAppName) {
        this.consumerAppName = consumerAppName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public ArrayList<ErrorService> getErrorServices() {
        return errorServices;
    }

    public void setErrorServices(ArrayList<ErrorService> errorServices) {
        this.errorServices = errorServices;
    }

    public ArrayList<ErrorEndpoint> getErrorEndpoints() {
        return errorEndpoints;
    }

    public void setErrorEndpoints(ArrayList<ErrorEndpoint> errorEndpoints) {
        this.errorEndpoints = errorEndpoints;
    }

    public ArrayList<ErrorLink> getErrorLinks() {
        return errorLinks;
    }

    public void setErrorLinks(ArrayList<ErrorLink> errorLinks) {
        this.errorLinks = errorLinks;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }

    public String getErrorUrl() {
        return errorUrl;
    }

    public void setErrorUrl(String errorUrl) {
        this.errorUrl = errorUrl;
    }

    public String getErrorMethod() {
        return errorMethod;
    }

    public void setErrorMethod(String errorMethod) {
        this.errorMethod = errorMethod;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public boolean isTestedPASS() {
        return testedPASS;
    }

    public void setTestedPASS(boolean testedPASS) {
        this.testedPASS = testedPASS;
    }
}
