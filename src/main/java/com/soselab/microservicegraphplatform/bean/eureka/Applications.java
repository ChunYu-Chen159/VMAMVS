package com.soselab.microservicegraphplatform.bean.eureka;

import java.util.ArrayList;

public class Applications {

    private String versions__delta;
    private String apps__hashcode;
    private ArrayList<Application> application;

    public Applications() {}

    public Applications(String versions__delta, String apps__hashcode, ArrayList<Application> application) {
        this.versions__delta = versions__delta;
        this.apps__hashcode = apps__hashcode;
        this.application = application;
    }

    public String getVersions__delta() {
        return versions__delta;
    }

    public void setVersions__delta(String versions__delta) {
        this.versions__delta = versions__delta;
    }

    public String getApps__hashcode() {
        return apps__hashcode;
    }

    public void setApps__hashcode(String apps__hashcode) {
        this.apps__hashcode = apps__hashcode;
    }

    public ArrayList<Application> getApplication() {
        return application;
    }

    public void setApplication(ArrayList<Application> application) {
        this.application = application;
    }

}
