package com.soselab.microservicegraphplatform.bean.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NodeEntity
public class Endpoint {

    @GraphId
    private Long id;

    private String endpointId;
    private String systemName;
    private String appName;
    private String method;
    private String path;

    public Endpoint() {
    }

    public Endpoint(String systemName, String appName, String method, String path) {
        this.systemName = systemName;
        this.appName = appName;
        this.method = method;
        this.path = path;
        this.endpointId = method + ":" + path;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getId() {
        return id;
    }

    @Relationship(type = "OWN", direction = Relationship.INCOMING)
    private Service service;

    public void ownBy(Service service) {
        this.service = service;
    }

    public Service getOwner() {
        return service;
    }

    @Relationship(type = "HTTP_REQUEST", direction = Relationship.OUTGOING)
    private Set<Endpoint> httpRequestEndpoints;

    public void httpRequestToEndpoint(Endpoint endpoint) {
        if (httpRequestEndpoints == null) {
            httpRequestEndpoints = new HashSet<>();
        }
        httpRequestEndpoints.add(endpoint);
    }

    public Set<Endpoint> getHttpRequestEndpoints() {
        return httpRequestEndpoints;
    }

    @Relationship(type = "AMQP_PUBLISH", direction = Relationship.INCOMING)
    private Set<Queue> amqpPublishQueues;

    public void amqpPublishToQueue(Queue queue) {
        if (amqpPublishQueues == null) {
            amqpPublishQueues = new HashSet<>();
        }
        amqpPublishQueues.add(queue);
    }

    public void amqpPublishToQueue(List<Queue> queue) {
        if (amqpPublishQueues == null) {
            amqpPublishQueues = new HashSet<>();
        }
        amqpPublishQueues.addAll(queue);
    }

    @Relationship(type = "AMQP_SUBSCRIBE", direction = Relationship.OUTGOING)
    private Set<Queue> amqpSubscribeQueues;

    public void amqpSubscribeToQueue(Queue queue) {
        if (amqpSubscribeQueues == null) {
            amqpSubscribeQueues = new HashSet<>();
        }
        amqpSubscribeQueues.add(queue);
    }

    public void amqpSubscribeToQueue(List<Queue> queue) {
        if (amqpSubscribeQueues == null) {
            amqpSubscribeQueues = new HashSet<>();
        }
        amqpSubscribeQueues.addAll(queue);
    }

}
