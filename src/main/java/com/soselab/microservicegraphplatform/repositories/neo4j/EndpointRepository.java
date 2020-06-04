package com.soselab.microservicegraphplatform.repositories.neo4j;

import com.soselab.microservicegraphplatform.bean.neo4j.Endpoint;
import com.soselab.microservicegraphplatform.bean.neo4j.NullEndpoint;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EndpointRepository extends Neo4jRepository<Endpoint, Long> {

    Endpoint findByEndpointId(String endpointId);

    @Query("MATCH (e:Endpoint)<-[:OWN]-(m:Service) WHERE m.appId = {appId} AND e.endpointId = {endpointId} RETURN e")
    Endpoint findByEndpointIdAndAppId(@Param("endpointId") String endpointId, @Param("appId") String appId);

    @Query("MATCH (e:NullEndpoint)<-[:OWN]-(m:Service) WHERE m.appId = {appId} AND e.endpointId = {endpointId} RETURN e")
    NullEndpoint findByNullEndpointAndAppId(@Param("endpointId") String endpointId, @Param("appId") String appId);

    @Query("MATCH (sm:Service)-[:REGISTER]->(:ServiceRegistry)<-[:REGISTER]-(tm:Service)-[:OWN]->(te:Endpoint) " +
            "WHERE sm.appId = {smId} AND tm.appName = {tmName} AND tm.version = {tmVer} AND te.endpointId = {teId} RETURN te")
    Endpoint findTargetEndpoint(@Param("smId") String sourceAppId, @Param("tmName") String targetAppName,
                                @Param("tmVer") String targetVersion, @Param("teId") String targetEndpointId);

    /*
    @Query("MATCH (tm:Service {appId:{tmAppId}})-[:OWN]->(te:Endpoint {endpointId}:{teId}) RETURN te")
    Endpoint findTargetEndpoint(@Param("tmId") String targetAppId, @Param("teId") String targetEndpointId);
*/
    @Query("MATCH (:Service {appId:{appId}})-[:OWN]->(e:Endpoint) RETURN e")
    List<Endpoint> findByAppId(@Param("appId") String appId);

    @Query("MATCH (s:Service {appId:{appId}})-[:OWN]->(e:Endpoint {path:{endpointPath}}) RETURN ID(e)")
    int findIdByAppIdAndEnpointPath(@Param("appId") String appId, @Param("endpointPath") String endpointPath);

    @Query("MATCH (sm:Service)-[:REGISTER]->(:ServiceRegistry)<-[:REGISTER]-(tm:Service)-[:OWN]->(te:Endpoint) " +
            "WHERE sm.appId = {smId} AND tm.appName = {tmName} AND te.endpointId = {teId} RETURN te")
    List<Endpoint> findTargetEndpointNotSpecVer(@Param("smId") String sourceAppId, @Param("tmName") String targetAppName,
                                               @Param("teId") String targetEndpointId);

    @Query("MATCH (ne:NullEndpoint) WHERE NOT (ne)<-[:HTTP_REQUEST]-() DETACH DELETE ne")
    void deleteUselessNullEndpoint();

    @Query("MATCH (:Service {appId:{appId}})-[:OWN]->(e:NullEndpoint {endpointId:{endpointId}}) REMOVE e:NullEndpoint")
    void removeNullLabelByAppIdAAndEndpointId(@Param("appId") String appId, @Param("endpointId") String endpointId);

}
