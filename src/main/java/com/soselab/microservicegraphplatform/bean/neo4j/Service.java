package com.soselab.microservicegraphplatform.bean.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Labels;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NodeEntity
public class Service {
    public static final String LABEL_HEAVY_STRONG_UPPER_DEPENDENCY = "HeavyStrongUpperDependency";
    public static final String LABEL_HEAVY_STRONG_LOWER_DEPENDENCY = "HeavyStrongLowerDependency";
    public static final String LABEL_HEAVY_WEAK_UPPER_DEPENDENCY = "HeavyWeakUpperDependency";
    public static final String LABEL_HEAVY_WEAK_LOWER_DEPENDENCY = "HeavyWeakLowerDependency";

    @GraphId
    private Long id;
    @Labels
    private List<String> labels = new ArrayList<>();

    private String appId;
    private String systemName;
    private String appName;
    private String version;
    private int number;
    private double errorProbability;

    public Service(){}

    public Service(@Nullable String systemName, String appName, @Nullable String version, int number) {
        this.appId = systemName + ":" + appName + ":" + version;
        this.systemName = systemName;
        this.appName = appName;
        this.version = version;
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public double getErrorProbability() {
        return errorProbability;
    }

    public void setErrorProbability(double errorProbability) {
        this.errorProbability = errorProbability;
    }

    public boolean addLabel(String label) {
        if (!labels.contains(label)) {
            labels.add(label);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeLabel(String label) {
        if (labels.contains(label)) {
            labels.remove(label);
            return true;
        } else {
            return false;
        }
    }

    @Relationship(type = "REGISTER", direction = Relationship.OUTGOING)
    private ServiceRegistry serviceRegistry;

    public void registerTo(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Relationship(type = "OWN", direction = Relationship.OUTGOING)
    private Set<Endpoint> endpoints;

    public void ownEndpoint(Endpoint endpoint) {
        if (endpoints == null) {
            endpoints = new HashSet<>();
        }
        endpoints.add(endpoint);
    }

    public void ownEndpoint(List<Endpoint> endpoints) {
        if (this.endpoints == null) {
            this.endpoints = new HashSet<>();
        }
        this.endpoints.addAll(endpoints);
    }

    public Set<Endpoint> getOwnEndpoints() {
        return endpoints;
    }

    @Relationship(type = "HTTP_REQUEST", direction = Relationship.OUTGOING)
    private Set<Endpoint> httpRequestEndpoints;

    public void httpRequestToEndpoint(Endpoint endpoint) {
        if (httpRequestEndpoints == null) {
            httpRequestEndpoints = new HashSet<>();
        }
        httpRequestEndpoints.add(endpoint);
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

    @Relationship(type = "NEWER_PATCH_VERSION", direction = Relationship.OUTGOING)
    private Service newerPatchVersion;

    public void foundNewPatchVersion(Service service) {
        newerPatchVersion = service;
    }

    @Relationship(type = "NEWER_PATCH_VERSION", direction = Relationship.INCOMING)
    private Set<Service> olderPatchVersion;

    public void foundOldPatchVersion(Service service) {
        if (olderPatchVersion == null) {
            olderPatchVersion = new HashSet<>();
        }
        olderPatchVersion.add(service);
    }

    public void foundOldPatchVersions(List<Service> service) {
        if (olderPatchVersion == null) {
            olderPatchVersion = new HashSet<>();
        }
        olderPatchVersion.addAll(service);
    }

    @Relationship(type = "MGP_CONFIG", direction = Relationship.OUTGOING)
    private Setting setting;

    public Setting getSetting() {
        return setting;
    }

    public void setSetting(Setting setting) {
        this.setting = setting;
    }


}
