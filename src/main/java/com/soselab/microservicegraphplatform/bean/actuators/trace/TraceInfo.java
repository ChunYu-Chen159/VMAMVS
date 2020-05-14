package com.soselab.microservicegraphplatform.bean.actuators.trace;

public class TraceInfo {

    private String method;
    private String path;
    private Headers headers;
    private String timeTaken;

    public TraceInfo() {
    }

    public TraceInfo(String method, String path, Headers headers, String timeTaken) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.timeTaken = timeTaken;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public String getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(String timeTaken) {
        this.timeTaken = timeTaken;
    }
}
