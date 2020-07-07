package com.soselab.microservicegraphplatform.bean.mgp.monitor.error;

import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorEndpoint;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorLink;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorService;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;

import java.util.ArrayList;

public class NodeError extends MonitorError {
    ArrayList<ErrorService> errorServiceList = new ArrayList<>();
    ArrayList<ErrorEndpoint> errorEndpointList = new ArrayList<>();
    ArrayList<ErrorLink> errorLinkList = new ArrayList<>();

    public NodeError(){
        super();

        setErrorAppId("CINEMA:PAYMENT:0.0.1-SNAPSHOT");
        setErrorSystemName("CINEMA");
        setErrorAppName("payment");
        setErrorAppVersion("0.0.1-SNAPSHOT");
        setConsumerAppName("ordering");
        setStatusCode("500");
        setErrorMessage("Request processing failed; nested exception is java.lang.NumberFormatException: For input string: \"\"");
        setErrorPath("/payment");
        setErrorUrl("http://140.121.197.128:4106/payment?userID=&price=250");
        setErrorMethod("GET");
        setErrorType("NodeError");
        setTestedPASS(true);
        errorServiceList.add(new ErrorService(477, "PAYMENT", "0.0.1-SNAPSHOT", "CINEMA:PAYMENT:0.0.1-SNAPSHOT", true));
        errorServiceList.add(new ErrorService(103, "ORDERING", "0.0.1-SNAPSHOT", "CINEMA:ORDERING:0.0.1-SNAPSHOT", false));
        errorServiceList.add(new ErrorService(440, "CINEMACATALOG", "0.0.1-SNAPSHOT", "CINEMA:CINEMACATALOG:0.0.1-SNAPSHOT", false));
        setErrorServices(errorServiceList);

        errorEndpointList.add(new ErrorEndpoint(416, "CINEMA:ORDERING:0.0.1-SNAPSHOT", "ORDERING", "/newMovieOrdering", false));
        errorEndpointList.add(new ErrorEndpoint(476, "CINEMA:PAYMENT:0.0.1-SNAPSHOT", "PAYMENT", "/payment", true));
        errorEndpointList.add(new ErrorEndpoint(414, "CINEMA:CINEMACATALOG:0.0.1-SNAPSHOT", "CINEMACATALOG", "/orderingMovie", false));
        setErrorEndpoints(errorEndpointList);

        errorLinkList.add(new ErrorLink(436, 440, "OWN", 414, false));
        errorLinkList.add(new ErrorLink(631, 416, "HTTP_REQUEST", 476, false));
        errorLinkList.add(new ErrorLink(320, 477, "OWN", 476, true));
        errorLinkList.add(new ErrorLink(411, 103, "OWN", 416, false));
        errorLinkList.add(new ErrorLink(67, 414, "HTTP_REQUEST", 416, false));
        setErrorLinks(errorLinkList);
    }
}
