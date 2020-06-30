package com.soselab.microservicegraphplatform.bean.mgp.monitor;

public class ErrorEndpoint {
    private long id;
    private boolean sourceOfError;
    private String parentAppId;
    private String parentAppName;
    private String path;

    public ErrorEndpoint(long id, boolean sourceOfError, String parentAppId, String parentAppName, String path) {
        this.id = id;
        this.sourceOfError = sourceOfError;
        this.parentAppId = parentAppId;
        this.parentAppName = parentAppName;
        this.path = path;
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

    public boolean isSourceOfError() {
        return sourceOfError;
    }

    public void setSourceOfError(boolean sourceOfError) {
        this.sourceOfError = sourceOfError;
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
}
