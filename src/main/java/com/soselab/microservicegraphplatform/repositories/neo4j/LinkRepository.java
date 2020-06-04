package com.soselab.microservicegraphplatform.repositories.neo4j;

import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface LinkRepository extends Neo4jRepository<Service, Long> {

    @Query("MATCH (n {systemName:{systemName}}) WHERE ID(n) = {Aid} " +
            "MATCH (n:Service)-[r:OWN]->(p:Endpoint) WHERE ID(p) = {Bid} " +
            "RETURN ID(r)")
    int findLinkIdBySystemNameAndAidAndBidWithOwn(@Param("systemName") String systemName, @Param("Aid") int AId, @Param("Bid") int BId);


    @Query("MATCH (n {systemName:{systemName}}) WHERE ID(n) = {Aid} " +
            "MATCH (n:Endpoint)-[r:HTTP_REQUEST]->(p:Endpoint) WHERE ID(p) = {Bid} " +
            "RETURN ID(r)")
    int findLinkIdBySystemNameAndAidAndBidWithHttpRequest(@Param("systemName") String systemName, @Param("Aid") int AId, @Param("Bid") int BId);
}
