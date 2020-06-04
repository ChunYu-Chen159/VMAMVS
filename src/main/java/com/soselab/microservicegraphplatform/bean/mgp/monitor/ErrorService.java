package com.soselab.microservicegraphplatform.bean.mgp.monitor;

// 錯誤路徑儲存
public class ErrorService {

    private int id;
    private String appName;
    private String version;
    private String appId;

    public ErrorService(int id, String appName, String version, String appId) {
        this.id = id;
        this.appName = appName;
        this.version = version;
        this.appId = appId;
    }


    public int getId() {
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
