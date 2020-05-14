package com.soselab.microservicegraphplatform.repositories.neo4j;

import com.soselab.microservicegraphplatform.bean.neo4j.Instance;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface InstanceRepository extends Neo4jRepository<Instance, Long> {

    Instance findByInstanceId(String instanceId);

    @Query("MATCH (n:Instance) WHERE n.instanceId = {instanceId} DETACH DELETE n")
    Instance deleteByInstanceId(@Param("instanceId") String instanceId);

    @Query("MATCH (n:Instance)<-[:OWN]-(s:ServiceRegistry) WHERE s.appId = {appId} RETURN n")
    ArrayList<Instance> findByServiceRegistryAppId(@Param("appId") String appId);

    Instance findByHostName(String hostName);

    Instance findByAppName(String appName);

    Instance findByIpAddr(String ipAddr);

    Instance findByPort(int port);

}
