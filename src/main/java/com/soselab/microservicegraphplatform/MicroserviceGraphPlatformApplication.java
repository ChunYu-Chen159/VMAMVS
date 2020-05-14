package com.soselab.microservicegraphplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableNeo4jRepositories
@EnableTransactionManagement
@EnableScheduling
public class MicroserviceGraphPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceGraphPlatformApplication.class, args);
    }

}

