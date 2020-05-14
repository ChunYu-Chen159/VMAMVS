package com.soselab.microservicegraphplatform.bean.actuators.trace;

public class Headers {

    private Request request;
    private Response response;

    public Headers() {
    }

    public Headers(Request request, Response response) {
        this.request = request;
        this.response = response;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

}
