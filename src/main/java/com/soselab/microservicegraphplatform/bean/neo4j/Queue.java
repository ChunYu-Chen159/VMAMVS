package com.soselab.microservicegraphplatform.bean.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NodeEntity
public class Queue {

    @GraphId
    private Long id;

    private String queueId;
    private String systemName;
    private String queueName;

    public Queue() {
    }

    public Queue(String systemName, String queueName) {
        this.queueId = systemName + ":" + queueName;
        this.systemName = systemName;
        this.queueName = queueName;
    }

    public Long getId() {
        return id;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Relationship(type = "AMQP_PUBLISH", direction = Relationship.OUTGOING)
    private Set<Queue> amqpPublishQueues;

    public void amqpPublishByQueue(Queue queue) {
        if (amqpPublishQueues == null) {
            amqpPublishQueues = new HashSet<>();
        }
        amqpPublishQueues.add(queue);
    }

    public void amqpPublishByQueue(List<Queue> queue) {
        if (amqpPublishQueues == null) {
            amqpPublishQueues = new HashSet<>();
        }
        amqpPublishQueues.addAll(queue);
    }

    @Relationship(type = "AMQP_SUBSCRIBE", direction = Relationship.INCOMING)
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
