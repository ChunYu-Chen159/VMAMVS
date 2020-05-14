package com.soselab.microservicegraphplatform.repositories.elasticsearch;

import com.soselab.microservicegraphplatform.bean.elasticsearch.MgpLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HttpRequestAndResponseRepository extends ElasticsearchRepository<MgpLog,String> {

    @Query("{\"bool\":{\"must\":[{\"match\":{\"systemName\":\"?0\"}},{\"match\":{\"appName\":\"?1\"}},{\"match\":{\"version\":\"?2\"}},{\"match\":{\"logger_name.keyword\":\"org.zalando.logbook.Logbook\"}}]}}")
    List<MgpLog> findBySystemNameAndAppNameAndVersion(String systemName, String appName, String version);

    @Query("{\"bool\":{\"must\":[{\"match\":{\"systemName\":\"?0\"}},{\"match\":{\"appName\":\"?1\"}},{\"match\":{\"version\":\"?2\"}},{\"match\":{\"logger_name.keyword\":\"org.zalando.logbook.Logbook\"}},{\"match\":{\"message\":\"response\"}}]}}")
    List<MgpLog> findResponseBySystemNameAndAppNameAndVersion(String systemName, String appName, String version, Pageable pageable);

    @Query("{\"bool\":{\"must\":[{\"range\":{\"@timestamp\":{\"gt\":\"now-1d\"}}},{\"match\":{\"systemName\":\"?0\"}},{\"match\":{\"appName\":\"?1\"}},{\"match\":{\"version\":\"?2\"}},{\"match\":{\"level.keyword\":\"ERROR\"}}]}}")
    List<MgpLog> findErrorBySystemNameAndAppNameAndVersion(String systemName, String appName, String version);

    @Query("{\"bool\":{\"must\":[{\"range\":{\"@timestamp\":{\"gt\":\"now-?3m\"}}},{\"match\":{\"systemName\":\"?0\"}},{\"match\":{\"appName\":\"?1\"}},{\"match\":{\"version\":\"?2\"}},{\"match\":{\"logger_name.keyword\":\"org.zalando.logbook.Logbook\"}},{\"match\":{\"message\":\"request\"}}]}}")
    List<MgpLog> findRecentMinutesRequestBySystemNameAndAppNameAndVersion(String systemName, String appName, String version, int minutes, Pageable pageable);

}
