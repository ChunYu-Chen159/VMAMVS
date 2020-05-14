package com.soselab.microservicegraphplatform.bean.eureka;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Port {

    private int $;
    private String enabled;

    public Port() {}

    public Port(int $, String enabled) {
        this.$ = $;
        this.enabled = enabled;
    }

    public int get$() {
        return $;
    }

    public void set$(int $) {
        this.$ = $;
    }

    @JsonProperty("@enabled")
    public String getEnabled() {
        return enabled;
    }

    @JsonProperty("@enabled")
    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

}
