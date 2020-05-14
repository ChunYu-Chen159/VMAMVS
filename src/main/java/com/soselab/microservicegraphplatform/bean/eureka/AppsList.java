package com.soselab.microservicegraphplatform.bean.eureka;

public class AppsList {

    private Applications applications;

    public AppsList() {}

    public AppsList(Applications applications) {
        this.applications = applications;
    }

    public Applications getApplications() {
        return applications;
    }

    public void setApplications(Applications applications) {
        this.applications = applications;
    }
}
