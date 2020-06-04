package com.soselab.microservicegraphplatform.bean.mgp.monitor;

public class ErrorLink {
    private long id;
    private long AId;
    private String realationship;
    private long BId;


    public ErrorLink(long id, long AId, String realationship, long BId) {
        this.id = id;
        this.AId = AId;
        this.realationship = realationship;
        this.BId = BId;
    }
}
