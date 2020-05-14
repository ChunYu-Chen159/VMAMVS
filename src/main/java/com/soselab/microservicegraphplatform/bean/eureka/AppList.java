package com.soselab.microservicegraphplatform.bean.eureka;

public class AppList {

    private Application application;

    public AppList() {
    }

    public AppList(Application application) {
        this.application = application;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }
}
