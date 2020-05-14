package com.soselab.microservicegraphplatform.bean.actuators.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Request {

    private String accept;
    @JsonProperty("user-agent")
    private String userAgent;
    private String host;
    private String connection;

    public Request() {
    }

    public Request(String accept, String userAgent, String host, String connection) {
        this.accept = accept;
        this.userAgent = userAgent;
        this.host = host;
        this.connection = connection;
    }

    public String getAccept() {
        return accept;
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

}
