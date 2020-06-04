package com.soselab.microservicegraphplatform.bean.mgp.monitor;

public class ErrorLink {
    private int id;
    private int AId;
    private String realationship;
    private int BId;


    public ErrorLink(int id, int AId, String realationship, int BId) {
        this.id = id;
        this.AId = AId;
        this.realationship = realationship;
        this.BId = BId;
    }
}
