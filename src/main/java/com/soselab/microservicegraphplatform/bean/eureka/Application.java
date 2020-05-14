package com.soselab.microservicegraphplatform.bean.eureka;

import java.util.ArrayList;

public class Application {

    private String name;
    private ArrayList<AppInstance> instance;

    public Application() {}

    public Application(String name, ArrayList<AppInstance> instance) {
        this.name = name;
        this.instance = instance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<AppInstance> getInstance() {
        return instance;
    }

    public void setInstance(ArrayList<AppInstance> instance) {
        this.instance = instance;
    }
}
