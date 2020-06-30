package com.soselab.microservicegraphplatform.bean.mgp.monitor;

public class ErrorEndpoint {
    private long id;
    private boolean isSourceOfError;
    private String parentAppId;
    private String parentAppName;
    private String path;

    public ErrorEndpoint(long id, boolean isSourceOfError, String parentAppId, String parentAppName, String path) {
        this.id = id;
        this.isSourceOfError = isSourceOfError;
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
        return isSourceOfError;
    }

    public void setSourceOfError(boolean sourceOfError) {
        isSourceOfError = sourceOfError;
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
