package com.soselab.microservicegraphplatform.bean.mgp.monitor;

public class ErrorEndpoint {
    private long id;
    private String parentAppId;
    private String parentAppName;
    private String path;
    private boolean sourceOfError;

    public ErrorEndpoint(long id, String parentAppId, String parentAppName, String path) {
        this.id = id;
        this.parentAppId = parentAppId;
        this.parentAppName = parentAppName;
        this.path = path;
        this.sourceOfError = false;
    }

    public ErrorEndpoint(long id, String parentAppId, String parentAppName, String path, boolean sourceOfError) {
        this.id = id;
        this.parentAppId = parentAppId;
        this.parentAppName = parentAppName;
        this.path = path;
        this.sourceOfError = sourceOfError;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setParentAppId(String parentAppId) {
        this.parentAppId = parentAppId;
    }

    public void setParentAppName(String parentAppName) {
        this.parentAppName = parentAppName;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getId() {
        return id;
    }

    public String getParentAppId() {
        return parentAppId;
    }

    public String getParentAppName() {
        return parentAppName;
    }

    public String getPath() {
        return path;
    }

    public boolean isSourceOfError() {
        return sourceOfError;
    }

    public void setSourceOfError(boolean sourceOfError) {
        this.sourceOfError = sourceOfError;
    }
}
