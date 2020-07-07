package com.soselab.microservicegraphplatform.bean.mgp.monitor.error;

import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorEndpoint;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorLink;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.ErrorService;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;

import java.util.ArrayList;

public class ResponseError extends MonitorError {
    ArrayList<ErrorService> errorServiceList = new ArrayList<>();
    ArrayList<ErrorEndpoint> errorEndpointList = new ArrayList<>();
    ArrayList<ErrorLink> errorLinkList = new ArrayList<>();


    public ResponseError() {
        super();

        setErrorAppId("CINEMA:ORDERING:0.0.1-SNAPSHOT");
        setErrorSystemName("CINEMA");
        setErrorAppName("ordering");
        setErrorAppVersion("0.0.1-SNAPSHOT");
        setConsumerAppName("groceryinventory");
        setStatusCode("500");
        setErrorMessage("Request processing failed; nested exception is java.lang.ArithmeticException: / by zero");
        setErrorPath("/newGroceryOrdering");
        setErrorUrl("http://140.121.197.128:4105/newGroceryOrdering?userID=2&groceryID=5c49e70e212d8d18c0fccd55&quantity=2");
        setErrorMethod("GET");
        setErrorType("ResponseError");
        setTestedPASS(true);
        errorServiceList.add(new ErrorService(317, "GROCERYINVENTORY", "0.0.1-SNAPSHOT", "CINEMA:GROCERYINVENTORY:0.0.1-SNAPSHOT", false));
        errorServiceList.add(new ErrorService(103, "ORDERING", "0.0.1-SNAPSHOT", "CINEMA:ORDERING:0.0.1-SNAPSHOT", true));
        errorServiceList.add(new ErrorService(393, "NOTIFICATION", "0.0.1-SNAPSHOT", "CINEMA:NOTIFICATION:0.0.1-SNAPSHOT", false));
        errorServiceList.add(new ErrorService(477, "PAYMENT", "0.0.1-SNAPSHOT", "CINEMA:PAYMENT:0.0.1-SNAPSHOT", false));
        setErrorServices(errorServiceList);

        errorEndpointList.add(new ErrorEndpoint(479, "CINEMA:GROCERYINVENTORY:0.0.1-SNAPSHOT", "GROCERYINVENTORY", "/orderingGrocery", false));
        errorEndpointList.add(new ErrorEndpoint(465, "CINEMA:ORDERING:0.0.1-SNAPSHOT", "ORDERING", "/newGroceryOrdering", true));
        errorEndpointList.add(new ErrorEndpoint(389, "CINEMA:NOTIFICATION:0.0.1-SNAPSHOT", "NOTIFICATION", "/newNotification", false));
        errorEndpointList.add(new ErrorEndpoint(476, "CINEMA:PAYMENT:0.0.1-SNAPSHOT", "PAYMENT", "/payment", false));
        setErrorEndpoints(errorEndpointList);


        errorLinkList.add(new ErrorLink(564, 317, "OWN", 479, false));
        errorLinkList.add(new ErrorLink(347, 479, "HTTP_REQUEST", 465, false));
        errorLinkList.add(new ErrorLink(410, 103, "OWN", 465, true));
        errorLinkList.add(new ErrorLink(628, 465, "HTTP_REQUEST", 389, false));
        errorLinkList.add(new ErrorLink(629, 465, "HTTP_REQUEST", 476, false));
        errorLinkList.add(new ErrorLink(365, 393, "OWN", 389, false));
        errorLinkList.add(new ErrorLink(320, 477, "OWN", 476, false));



        setErrorLinks(errorLinkList);


    }
}
