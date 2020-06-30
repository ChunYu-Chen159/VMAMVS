package com.soselab.microservicegraphplatform.bean.mgp.monitor;

// 錯誤路徑儲存
public class ErrorService {

    private long id;
    private boolean isSourceOfError;
    private String appName;
    private String version;
    private String appId;

    public ErrorService(long id, boolean isSourceOfError, String appName, String version, String appId) {
        this.id = id;
        this.isSourceOfError = isSourceOfError;
        this.appName = appName;
        this.version = version;
        this.appId = appId;
    }


    public void setId(long id) {
        this.id = id;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setAppId(String appId) {
        this.appId = appId;
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

    public String getAppName() {
        return appName;
    }

    public String getVersion() {
        return version;
    }

    public String getAppId() {
        return appId;
    }


}
