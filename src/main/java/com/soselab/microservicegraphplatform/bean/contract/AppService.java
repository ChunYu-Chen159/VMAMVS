package com.soselab.microservicegraphplatform.bean.contract;

import java.util.List;

public class AppService {

    private String appName;
    private String version;
    private List<APIs> apiList;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<APIs> getApiList() {
        return apiList;
    }

    public void setApiList(List<APIs> apiList) {
        this.apiList = apiList;
    }


}
