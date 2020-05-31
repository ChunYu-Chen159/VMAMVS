package com.soselab.microservicegraphplatform.repositories.neo4j;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneralRepository extends Neo4jRepository {

    @Query("MATCH (s:ServiceRegistry) WITH DISTINCT s.systemName as result RETURN result")
    List<String> getAllSystemName();

    @Query("MATCH (s:Service{systemName:{systemName}}) WITH DISTINCT s.appName as result RETURN result")
    List<String> getSystemAllServiceName(@Param("systemName") String systemName);

    @Query("MATCH (s:Service) WITH DISTINCT s.appName as result RETURN result")
    List<String> getSystemAllServiceName();

    @Query("MATCH (n)-[:HTTP_REQUEST]->(e:Endpoint) " +
            "OPTIONAL MATCH (n)<-[:OWN]-(parent:Service) " +
            "OPTIONAL MATCH (e)<-[:OWN]-(targetParent:Service) " +
            "WITH parent.version AS serviceVersion, targetParent.appName AS targetService, targetParent.version as targetServiceVersion , n.appName AS service, n.path AS path, n.method AS method " +
            "ORDER BY service RETURN apoc.convert.toJson({serviceVersion:serviceVersion, targetAppName:targetService, targetServiceVersion:targetServiceVersion, method:method, path:path, appName:service})")
    List<String> getAllServiceAndPathWithHTTP_REQUEST();


/*    @Query("MATCH (n:Service{appName:{appName}})-[:OWN]->(e:Endpoint) " +
            "OPTIONAL MATCH (e)-[:HTTP_REQUEST]->(e2:Endpoint) " +
            "OPTIONAL MATCH (e2)<-[:OWN]-(providerService:Service) " +
            "RETURN apoc.convert.toJson({services:[providerService]})")*/
    @Query("MATCH (n)-[:HTTP_REQUEST]->(e:Endpoint) " +
            "OPTIONAL MATCH (n)<-[:OWN]-(parent:Service) WHERE parent.appName = {aname} " +
            "OPTIONAL MATCH (e)<-[:OWN]-(targetParent:Service) " +
            "WITH [{id:id(targetParent), appId:targetParent.appId, appName:targetParent.appName, number:targetParent.number, systemName:targetParent.systemName, version:targetParent.version}] AS nodes " +
            "RETURN apoc.convert.toJson({services:[nodes]})")
    List<String> getAllHttpRequestServiceWithService(@Param("aname") String appName);

    @Query("MATCH (n) WHERE n:Service OR n:Endpoint OR n:Queue " +
            "MATCH ()-[r]->() WHERE (:Service)-[r:OWN]->(:Endpoint) OR ()-[r:HTTP_REQUEST]->() OR ()-[r:AMQP_PUBLISH]->() OR ()-[r:AMQP_SUBSCRIBE]-() " +
            "WITH collect(DISTINCT n) as ns, collect(DISTINCT r) as rs " +
            "WITH [node in ns | node {.*, id:id(node), labels:labels(node)}] as nodes, " +
            "[rel in rs | rel {.*, type:type(rel), " +
            "source:id(startNode(rel)), target:id(endNode(rel))}] as rels " +
            "RETURN apoc.convert.toJson({nodes:nodes, links:rels})")
    String getGraphJson();

    @Query("MATCH (n {systemName:{systemName}}) WHERE n:Service OR n:Endpoint OR n:Queue " +
            "MATCH (n)-[r]-() WHERE (:Service)-[r:OWN]->(:Endpoint) OR ()-[r:HTTP_REQUEST]->() OR ()-[r:AMQP_PUBLISH]->() OR ()-[r:AMQP_SUBSCRIBE]->() OR ()-[r:NEWER_PATCH_VERSION]->() " +
            "WITH collect(DISTINCT n) as ns, collect(DISTINCT r) as rs " +
            "WITH [node in ns | node {.*, id:id(node), labels:labels(node)}] as nodes, " +
            "[rel in rs | rel {.*, type:type(rel), " +
            "source:id(startNode(rel)), target:id(endNode(rel))}] as rels " +
            "RETURN apoc.convert.toJson({nodes:nodes, links:rels})")
    String getSystemGraphJson(@Param("systemName") String systemName);

    @Query("MATCH (n) WHERE ID(n) = {id} " +
            "CALL apoc.path.subgraphAll(n, {relationshipFilter:\"OWN>|HTTP_REQUEST>|AMQP_PUBLISH>|AMQP_SUBSCRIBE>\"}) YIELD nodes, relationships " +
            "WITH [node in nodes | node {id:id(node)}] as nodes, " +
            "[rel in relationships | rel {type:type(rel), source:id(startNode(rel)), target:id(endNode(rel))}] as rels " +
            "RETURN apoc.convert.toJson({nodes:nodes, links:rels})")
    String getStrongUpperDependencyChainById(@Param("id") Long id);

    @Query("MATCH (n {systemName:{systemName}}) WHERE ID(n) = {id} " +
            "CALL apoc.path.subgraphAll(n, {relationshipFilter:\"OWN>|HTTP_REQUEST>|AMQP_PUBLISH>|AMQP_SUBSCRIBE>\"}) YIELD nodes " +
            "RETURN size([s IN nodes WHERE s:Service]) - 1")
    Integer getStrongUpperDependencyServiceCountByIdAndSystemName(@Param("id") Long id, @Param("systemName") String systemName);

    @Query("MATCH (n) WHERE ID(n) = {id} " +
            "CALL apoc.path.subgraphAll(n, {relationshipFilter:\"OWN|HTTP_REQUEST>|AMQP_PUBLISH>|AMQP_SUBSCRIBE>\"}) YIELD nodes, relationships " +
            "WITH [node in nodes | node {id:id(node)}] as nodes, " +
            "[rel in relationships | rel {type:type(rel), source:id(startNode(rel)), target:id(endNode(rel))}] as rels " +
            "RETURN apoc.convert.toJson({nodes:nodes, links:rels})")
    String getWeakUpperDependencyChainById(@Param("id") Long id);

    @Query("MATCH (n {systemName:{systemName}}) WHERE ID(n) = {id} " +
            "CALL apoc.path.subgraphAll(n, {relationshipFilter:\"OWN|HTTP_REQUEST>|AMQP_PUBLISH>|AMQP_SUBSCRIBE>\"}) YIELD nodes " +
            "RETURN size([s IN nodes WHERE s:Service]) - 1")
    Integer getWeakUpperDependencyServiceCountByIdAndSystemName(@Param("id") Long id, @Param("systemName") String systemName);

    @Query("MATCH (n) WHERE ID(n) = {id} " +
            "CALL apoc.path.subgraphAll(n, {relationshipFilter:\"OWN>|<HTTP_REQUEST|<AMQP_PUBLISH|<AMQP_SUBSCRIBE\"}) YIELD nodes, relationships " +
            "WITH [node in nodes | node {id:id(node)}] as nodes, " +
            "[rel in relationships | rel {type:type(rel), source:id(startNode(rel)), target:id(endNode(rel))}] as rels " +
            "RETURN apoc.convert.toJson({nodes:nodes, links:rels})")
    String getStrongLowerDependencyChainById(@Param("id") Long id);

    @Query("MATCH (n {systemName:{systemName}}) WHERE ID(n) = {id} " +
            "CALL apoc.path.subgraphAll(n, {relationshipFilter:\"OWN>|<HTTP_REQUEST|<AMQP_PUBLISH|<AMQP_SUBSCRIBE\"}) YIELD nodes " +
            "RETURN size([s IN nodes WHERE s:Service]) - 1")
    Integer getStrongLowerDependencyServiceCountByIdAndSystemName(@Param("id") Long id, @Param("systemName") String systemName);

    @Query("MATCH (n) WHERE ID(n) = {id} " +
            "CALL apoc.path.subgraphAll(n, {relationshipFilter:\"OWN|<HTTP_REQUEST|<AMQP_PUBLISH|<AMQP_SUBSCRIBE\"}) YIELD nodes, relationships " +
            "WITH [node in nodes | node {id:id(node)}] as nodes, " +
            "[rel in relationships | rel {type:type(rel), source:id(startNode(rel)), target:id(endNode(rel))}] as rels " +
            "RETURN apoc.convert.toJson({nodes:nodes, links:rels})")
    String getWeakLowerDependencyChainById(@Param("id") Long id);

    @Query("MATCH (n {systemName:{systemName}}) WHERE ID(n) = {id} " +
            "CALL apoc.path.subgraphAll(n, {relationshipFilter:\"OWN|<HTTP_REQUEST|<AMQP_PUBLISH|<AMQP_SUBSCRIBE\"}) YIELD nodes " +
            "RETURN size([s IN nodes WHERE s:Service]) - 1")
    Integer getWeakLowerDependencyServiceCountByIdAndSystemName(@Param("id") Long id, @Param("systemName") String systemName);

    @Query("MATCH (n) WHERE ID(n) = {id}" +
            "OPTIONAL MATCH (n)-[:HTTP_REQUEST]->(p1) " +
            "OPTIONAL MATCH (n)-[:OWN]->(:Endpoint)-[:HTTP_REQUEST]->(p2) " +
            "OPTIONAL MATCH (n)-[:AMQP_SUBSCRIBE]->(p3) WHERE n:Service OR n:Endpoint " +
            "OPTIONAL MATCH (n)-[:OWN]->(:Endpoint)-[:AMQP_SUBSCRIBE]->(p4) " +
            "OPTIONAL MATCH (n)-[:AMQP_PUBLISH]->(p5) WHERE n:Queue " +
            "WITH [p1, p2, p3, p4, p5] as coll " +
            "UNWIND coll AS ps " +
            "WITH collect(DISTINCT ps) as ns " +
            "WITH [node in ns | node {id:id(node)}] as nodes " +
            "RETURN apoc.convert.toJson({nodes:nodes, links:[]})")
    String getProviders(@Param("id") Long appId);

    @Query("MATCH (n) WHERE ID(n) = {id}" +
            "OPTIONAL MATCH (n)<-[:HTTP_REQUEST]-(p1) " +
            "OPTIONAL MATCH (n)-[:OWN]->(:Endpoint)<-[:HTTP_REQUEST]-(p2) " +
            "OPTIONAL MATCH (n)<-[:AMQP_PUBLISH]-(p3) WHERE n:Service OR n:Endpoint " +
            "OPTIONAL MATCH (n)-[:OWN]->(:Endpoint)<-[:AMQP_PUBLISH]-(p4) " +
            "OPTIONAL MATCH (n)<-[:AMQP_SUBSCRIBE]-(p5) WHERE n:Queue " +
            "WITH [p1, p2, p3, p4, p5] as coll " +
            "UNWIND coll AS ps " +
            "WITH collect(DISTINCT ps) as ns " +
            "WITH [node in ns | node {id:id(node)}] as nodes " +
            "RETURN apoc.convert.toJson({nodes:nodes, links:[]})")
    String getConsumers(@Param("id") Long appId);

}
