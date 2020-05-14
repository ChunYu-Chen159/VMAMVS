package com.soselab.microservicegraphplatform.bean.neo4j;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class NullEndpoint extends Endpoint {

    public NullEndpoint() {
    }

    public NullEndpoint(String systemName, String appName, String method, String path) {
        super(systemName, appName, method, path);
    }

}
