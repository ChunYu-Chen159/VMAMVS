package com.soselab.microservicegraphplatform.bean.mgp;

import java.util.List;

public class AppMetrics {

    private int averageDuration;
    private List<Status> statuses;
    private long errorCount;

    public AppMetrics() {
    }

    public AppMetrics(int averageDuration, List<Status> statuses, long errorCount) {
        this.averageDuration = averageDuration;
        this.statuses = statuses;
        this.errorCount = errorCount;
    }

    public int getAverageDuration() {
        return averageDuration;
    }

    public void setAverageDuration(int averageDuration) {
        this.averageDuration = averageDuration;
    }

    public List<Status> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<Status> statuses) {
        this.statuses = statuses;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public float getFailureStatusRate() {
        float failureStatusRate = 1;
        for (Status status : this.statuses) {
            if (status.getCode() < 400) {
                failureStatusRate -= status.getRatio();
                break;
            }
        }
        return failureStatusRate;
    }

    public int getFailureStatusSamplesNum() {
        int count = 0;
        for (Status status : this.statuses) {
            count += status.getCount();
        }
        return count;
    }

    public int getDurationSamplesNum() {
        return getFailureStatusSamplesNum();
    }

}
