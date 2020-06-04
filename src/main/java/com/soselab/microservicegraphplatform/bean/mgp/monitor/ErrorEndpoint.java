package com.soselab.microservicegraphplatform.bean.mgp.monitor;

public class ErrorEndpoint {
    private int id;
    private String parentAppId;
    private String parentAppName;
    private String path;

    public ErrorEndpoint(int id, String parentAppId, String parentAppName, String path) {
        this.id = id;
        this.parentAppId = parentAppId;
        this.parentAppName = parentAppName;
        this.path = path;
    }

    public int getId() {
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
