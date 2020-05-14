package com.soselab.microservicegraphplatform.repositories.neo4j;

import com.soselab.microservicegraphplatform.bean.neo4j.Setting;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends Neo4jRepository<Setting, Long> {

    @Query("MATCH (s:Setting)<-[r:MGP_CONFIG]-(n:Service {appId:{appId}}) RETURN s, r, n")
    Setting findByConfigServiceAppId(@Param("appId") String appId);

}
