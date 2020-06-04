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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAId() {
        return AId;
    }

    public void setAId(long AId) {
        this.AId = AId;
    }

    public String getRealationship() {
        return realationship;
    }

    public void setRealationship(String realationship) {
        this.realationship = realationship;
    }

    public long getBId() {
        return BId;
    }

    public void setBId(long BId) {
        this.BId = BId;
    }
}
