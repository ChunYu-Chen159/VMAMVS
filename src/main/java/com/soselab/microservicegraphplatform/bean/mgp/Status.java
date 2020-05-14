package com.soselab.microservicegraphplatform.bean.mgp;

public class Status {

    private int code;
    private int count;
    private float ratio;

    public Status() {
    }

    public Status(int code, int count, float ratio) {
        this.code = code;
        this.count = count;
        this.ratio = ratio;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public float getRatio() {
        return ratio;
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }
}
