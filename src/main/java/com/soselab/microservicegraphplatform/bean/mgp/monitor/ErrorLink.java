package com.soselab.microservicegraphplatform.bean.mgp.monitor;

public class ErrorLink {
    private long id;
    private boolean isSourceOfError;
    private long AId;
    private String relationship;
    private long BId;


    public ErrorLink(long id, boolean isSourceOfError, long AId, String relationship, long BId) {
        this.id = id;
        this.isSourceOfError = isSourceOfError;
        this.AId = AId;
        this.relationship = relationship;
        this.BId = BId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean getIsSourceOfError() {
        return isSourceOfError;
    }

    public void setIsSourceOfError(boolean isSourceOfError) {
        this.isSourceOfError = isSourceOfError;
    }

    public long getAId() {
        return AId;
    }

    public void setAId(long AId) {
        this.AId = AId;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public long getBId() {
        return BId;
    }

    public void setBId(long BId) {
        this.BId = BId;
    }
}
