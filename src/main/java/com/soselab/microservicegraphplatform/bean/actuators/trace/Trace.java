package com.soselab.microservicegraphplatform.bean.actuators.trace;

public class Trace {

    private Long timestamp;
    private TraceInfo info;


    public Trace() {
    }

    public Trace(Long timestamp, TraceInfo info) {
        this.timestamp = timestamp;
        this.info = info;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public TraceInfo getInfo() {
        return info;
    }

    public void setInfo(TraceInfo info) {
        this.info = info;
    }

}
