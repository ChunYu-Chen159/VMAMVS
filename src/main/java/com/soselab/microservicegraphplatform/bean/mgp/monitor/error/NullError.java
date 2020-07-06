package com.soselab.microservicegraphplatform.bean.mgp.monitor.error;

import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorEndpoint;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorLink;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorService;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;

import java.util.ArrayList;
import java.util.List;

public class NullError extends MonitorError {

    ArrayList<ErrorService> errorServiceList = new ArrayList<>();
    ArrayList<ErrorEndpoint> errorEndpointList = new ArrayList<>();
    ArrayList<ErrorLink> errorLinkList = new ArrayList<>();


    public NullError(){
        super();

        setErrorAppId("CINEMA:CINEMACATALOG:0.0.1-SNAPSHOT");
        setErrorSystemName("CINEMA");
        setErrorAppName("cinemacatalog");
        setErrorAppVersion("0.0.1-SNAPSHOT");
        setConsumerAppName("");
        setStatusCode("500");
        setErrorMessage("Request processing failed; nested exception is feign.FeignException: status 404 reading OrderingInterface#getSomething(String); content:\n" +
                "{\"timestamp\":1593954502449,\"status\":404,\"error\":\"Not Found\",\"message\":\"No message available\",\"path\":\"/getSomething\"}\",\n" +
                "errorPath: \"/getSomething");
        setErrorPath("/getSomething");
        setErrorUrl("http://140.121.197.128:4104/getSomething?userID=2");
        setErrorMethod("GET");
        setErrorType("NullError");
        setTestedPASS(false);
        errorServiceList.add(new ErrorService(103, "ORDERING", "0.0.1-SNAPSHOT", "CINEMA:ORDERING:0.0.1-SNAPSHOT", false));
        errorServiceList.add(new ErrorService(440, "CINEMACATALOG", "0.0.1-SNAPSHOT", "CINEMA:CINEMACATALOG:0.0.1-SNAPSHOT", true));
        setErrorServices(errorServiceList);

        errorEndpointList.add(new ErrorEndpoint(435, "CINEMA:CINEMACATALOG:0.0.1-SNAPSHOT", "CINEMACATALOG", "/getSomething", true));
        errorEndpointList.add(new ErrorEndpoint(405, "CINEMA:ORDERING:0.0.1-SNAPSHOT", "ORDERING", "/getSomething", false));
        setErrorEndpoints(errorEndpointList);

        errorLinkList.add(new ErrorLink(440, 103, "OWN", 405, false));
        errorLinkList.add(new ErrorLink(633, 435, "HTTP_REQUEST", 405, false));
        errorLinkList.add(new ErrorLink(437, 440, "OWN", 435, true));
        setErrorLinks(errorLinkList);
    }
}
