package com.soselab.microservicegraphplatform.repositories.neo4j;

import com.soselab.microservicegraphplatform.bean.neo4j.NullService;
import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import com.sun.tools.javac.resources.version;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SleuthRepository extends Neo4jRepository<Service, Long> {

    @Query("MATCH (s:ServiceRegistry) WITH DISTINCT s.systemName as result RETURN result")
    List<String> getAllSystemName();

    @Query("MATCH (s:Service{systemName:{systemName}}) WITH DISTINCT s.appName as result RETURN result")
    List<String> getSystemAllServiceName(@Param("systemName") String systemName);

    @Query("MATCH (n)-[r:HTTP_REQUEST]->() " +
            "RETURN n.appName AS service, n.path AS path, n.method AS method ORDER BY service")
    String getServiceWithHTTP_REQUEST();

    @Query("MATCH (n)-[:HTTP_REQUEST]->(e:Endpoint) " +
            "OPTIONAL MATCH (n)<-[:OWN]-(parent:Service) " +
            "OPTIONAL MATCH (e)<-[:OWN]-(targetParent:Service) " +
            "WITH parent.version AS serviceVersion, targetParent.appName AS targetService, targetParent.version as targetServiceVersion , n.appName AS service, n.path AS path, n.method AS method " +
            "ORDER BY service RETURN apoc.convert.toJson({serviceVersion:serviceVersion, targetAppName:targetService, targetServiceVersion:targetServiceVersion, method:method, path:path, appName:service})")
    List<String> getAllServiceAndPathWithHTTP_REQUEST();

}
