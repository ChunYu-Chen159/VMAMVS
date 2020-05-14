package com.soselab.microservicegraphplatform.bean.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Instance {

    @GraphId
    private Long id;

    private String instanceId;
    private String appName;
    private String hostName;
    private String ipAddr;
    private int port;

    public Instance() {}

    public Instance(String appName, String hostName, String ipAddr, int port) {
        this.instanceId = hostName + ":" + appName + ":" + port;
        this.appName = appName;
        this.hostName = hostName;
        this.ipAddr = ipAddr;
        this.port = port;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Relationship(type = "OWN", direction = Relationship.INCOMING)
    public ServiceRegistry serviceRegistry;

    public void ownBy(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

}
