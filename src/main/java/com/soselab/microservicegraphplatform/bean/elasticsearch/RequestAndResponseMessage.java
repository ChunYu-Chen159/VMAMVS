package com.soselab.microservicegraphplatform.bean.elasticsearch;

public class RequestAndResponseMessage {

    private String origin;
    private String type;
    private String correlation;
    private String protocol;
    private String remote;
    private String method;
    private String uri;
    private int duration;
    private int status;

    public RequestAndResponseMessage() {
    }

    public RequestAndResponseMessage(String origin, String type, String correlation, String protocol, String remote, String method, String uri, int duration, int status) {
        this.origin = origin;
        this.type = type;
        this.correlation = correlation;
        this.protocol = protocol;
        this.remote = remote;
        this.method = method;
        this.uri = uri;
        this.duration = duration;
        this.status = status;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCorrelation() {
        return correlation;
    }

    public void setCorrelation(String correlation) {
        this.correlation = correlation;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
