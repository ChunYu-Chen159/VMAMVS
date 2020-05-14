package com.soselab.microservicegraphplatform.bean.actuators.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {

    @JsonProperty("X-Application-Context")
    private String xApplicationContext;
    @JsonProperty("Content-Type")
    private String contentType;
    @JsonProperty("Transfer-Encoding")
    private String transferEncoding;
    @JsonProperty("Date")
    private String date;
    private String status;

    public Response() {
    }

    public Response(String xApplicationContext, String contentType, String transferEncoding, String date, String status) {
        this.xApplicationContext = xApplicationContext;
        this.contentType = contentType;
        this.transferEncoding = transferEncoding;
        this.date = date;
        this.status = status;
    }

    public String getxApplicationContext() {
        return xApplicationContext;
    }

    public void setxApplicationContext(String xApplicationContext) {
        this.xApplicationContext = xApplicationContext;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getTransferEncoding() {
        return transferEncoding;
    }

    public void setTransferEncoding(String transferEncoding) {
        this.transferEncoding = transferEncoding;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
