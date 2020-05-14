package com.soselab.microservicegraphplatform.bean.mgp;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class MgpApplication {

    private String appId;
    private String systemName;
    private String appName;
    private String version;
    private ArrayList<MgpInstance> instances;

    public MgpApplication() {
    }

    public MgpApplication(String systemName, String appName, String version, @Nullable ArrayList<MgpInstance> instances) {
        this.appId = systemName + ":" + appName + ":" + version;
        this.systemName = systemName;
        this.appName = appName;
        this.version = version;
        this.instances = instances;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

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

    public ArrayList<MgpInstance> getInstances() {
        return instances;
    }

    public void setInstances(ArrayList<MgpInstance> instances) {
        this.instances = instances;
    }

    public void addInstance(MgpInstance instance) {
        if (this.instances == null) {
            this.instances = new ArrayList<>();
        }
        this.instances.add(instance);
    }

}
