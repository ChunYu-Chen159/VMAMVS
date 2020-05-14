package com.soselab.microservicegraphplatform.bean.eureka;

public class LeaseInfo {

    private int renewalIntervalInSecs;
    private int durationInSecs;
    private Long registrationTimestamp;
    private Long lastRenewalTimestamp;
    private Long evictionTimestamp;
    private Long serviceUpTimestamp;

    public LeaseInfo() {}

    public LeaseInfo(int renewalIntervalInSecs, int durationInSecs, Long registrationTimestamp, Long lastRenewalTimestamp, Long evictionTimestamp, Long serviceUpTimestamp) {
        this.renewalIntervalInSecs = renewalIntervalInSecs;
        this.durationInSecs = durationInSecs;
        this.registrationTimestamp = registrationTimestamp;
        this.lastRenewalTimestamp = lastRenewalTimestamp;
        this.evictionTimestamp = evictionTimestamp;
        this.serviceUpTimestamp = serviceUpTimestamp;
    }

    public int getRenewalIntervalInSecs() {
        return renewalIntervalInSecs;
    }

    public void setRenewalIntervalInSecs(int renewalIntervalInSecs) {
        this.renewalIntervalInSecs = renewalIntervalInSecs;
    }

    public int getDurationInSecs() {
        return durationInSecs;
    }

    public void setDurationInSecs(int durationInSecs) {
        this.durationInSecs = durationInSecs;
    }

    public Long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public void setRegistrationTimestamp(Long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public Long getLastRenewalTimestamp() {
        return lastRenewalTimestamp;
    }

    public void setLastRenewalTimestamp(Long lastRenewalTimestamp) {
        this.lastRenewalTimestamp = lastRenewalTimestamp;
    }

    public Long getEvictionTimestamp() {
        return evictionTimestamp;
    }

    public void setEvictionTimestamp(Long evictionTimestamp) {
        this.evictionTimestamp = evictionTimestamp;
    }

    public Long getServiceUpTimestamp() {
        return serviceUpTimestamp;
    }

    public void setServiceUpTimestamp(Long serviceUpTimestamp) {
        this.serviceUpTimestamp = serviceUpTimestamp;
    }
}
