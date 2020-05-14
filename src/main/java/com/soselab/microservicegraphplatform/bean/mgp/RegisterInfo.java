package com.soselab.microservicegraphplatform.bean.mgp;

import java.util.ArrayList;

public class RegisterInfo {

    private ArrayList<MgpApplication> mgpApplications;

    public RegisterInfo() {
    }

    public RegisterInfo(ArrayList<MgpApplication> mgpApplications) {
        this.mgpApplications = mgpApplications;
    }

    public ArrayList<MgpApplication> getMgpApplications() {
        return mgpApplications;
    }

    public void setMgpApplications(ArrayList<MgpApplication> mgpApplications) {
        this.mgpApplications = mgpApplications;
    }

}
