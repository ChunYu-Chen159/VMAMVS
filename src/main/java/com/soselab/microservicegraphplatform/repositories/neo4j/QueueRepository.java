package com.soselab.microservicegraphplatform.repositories.neo4j;

import com.soselab.microservicegraphplatform.bean.neo4j.Queue;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueueRepository extends Neo4jRepository<Queue, Long> {

    Queue findByQueueId(String queueId);

    @Query("MATCH (q:Queue) WHERE NOT (q)<-[:AMQP_SUBSCRIBE]-() AND NOT (q)-[:AMQP_PUBLISH]->() DETACH DELETE q")
    void deleteUselessQueues();

}
