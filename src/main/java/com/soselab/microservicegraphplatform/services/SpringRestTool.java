package com.soselab.microservicegraphplatform.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soselab.microservicegraphplatform.bean.actuators.Info;
import com.soselab.microservicegraphplatform.bean.actuators.trace.Trace;
import com.soselab.microservicegraphplatform.bean.eureka.AppInstance;
import com.soselab.microservicegraphplatform.bean.eureka.AppList;
import com.soselab.microservicegraphplatform.bean.eureka.Application;
import com.soselab.microservicegraphplatform.bean.eureka.AppsList;
import com.soselab.microservicegraphplatform.bean.mgp.MgpApplication;
import com.soselab.microservicegraphplatform.bean.mgp.MgpInstance;
import com.soselab.microservicegraphplatform.bean.neo4j.Instance;
import com.soselab.microservicegraphplatform.bean.neo4j.ServiceRegistry;
import com.soselab.microservicegraphplatform.repositories.neo4j.InstanceRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRegistryRepository;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Configuration
public class SpringRestTool {
    private static final Logger logger = LoggerFactory.getLogger(SpringRestTool.class);
    private final String EUREKA_V1_BASEPATH = "/eureka";
    private final String EUREKA_V2_BASEPATH = "/eureka/v2";
    private final String ACTUATOR_V1_BASEPATH = "";
    private final String ACTUATOR_V2_BASEPATH = "/actuator";

    @Autowired
    private ServiceRegistryRepository serviceRegistryRepository;
    @Autowired
    private InstanceRepository instanceRepository;
    @Autowired
    private ObjectMapper mapper;
    private RestTemplate restTemplate = new RestTemplate();

    // [Eureka]

    private AppsList getAppsListFromEureka(String appId) {
        AppsList appsList = null;
        ArrayList<Instance> instances = instanceRepository.findByServiceRegistryAppId(appId);
        if (instances.size() > 0) {
            Instance instance = instances.get(0);
            try {
                String url = "http://" + instance.getIpAddr() + ":" + instance.getPort() + EUREKA_V1_BASEPATH + "/apps/";
                appsList = restTemplate.getForObject(url, AppsList.class);
            } catch (ResourceAccessException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return appsList;
    }

    public MgpApplication getAppFromEureka(String systemName, String appName, String version) {
        ServiceRegistry serviceRegistry = serviceRegistryRepository.findBySystemName(systemName);
        List<Instance> registryInstance = instanceRepository.findByServiceRegistryAppId(serviceRegistry.getAppId());
        String registryUrl = "http://" + registryInstance.get(0).getIpAddr() + ":" + registryInstance.get(0).getPort() + EUREKA_V1_BASEPATH + "/apps/" + appName;
        AppList eurekaApp = restTemplate.getForObject(registryUrl, AppList.class);
        MgpApplication mgpApplication = null;
        for (AppInstance instance: eurekaApp.getApplication().getInstance()) {
            if (instance.getStatus().equals("UP")) {
                String url = "http://" + instance.getIpAddr() + ":" + instance.getPort().get$();
                String ver = restTemplate.getForObject( url + "/info", Info.class).getVersion();
                if (ver.equals(version)) {
                    if (mgpApplication == null) {
                        mgpApplication = new MgpApplication(serviceRegistry.getSystemName(), appName, version, new ArrayList<>());
                    }
                    MgpInstance mgpInstance = new MgpInstance(instance.getHostName(), appName, instance.getIpAddr(), instance.getPort().get$());
                    mgpApplication.addInstance(mgpInstance);
                }
            }
        }

        return mgpApplication;
    }

    public MgpApplication getAppFromEureka(ServiceRegistry serviceRegistry, String appName, String version) {
        List<Instance> registryInstance = instanceRepository.findByServiceRegistryAppId(serviceRegistry.getAppId());
        String registryUrl = "http://" + registryInstance.get(0).getIpAddr() + ":" + registryInstance.get(0).getPort() + EUREKA_V1_BASEPATH + "/apps/" + appName;
        AppList eurekaApp = restTemplate.getForObject
                (registryUrl, AppList.class);
        MgpApplication mgpApplication = null;
        for (AppInstance instance: eurekaApp.getApplication().getInstance()) {
            if (instance.getStatus().equals("UP")) {
                String url = "http://" + instance.getIpAddr() + ":" + instance.getPort().get$();
                String ver = restTemplate.getForObject( url + "/info", Info.class).getVersion();
                if (ver.equals(version)) {
                    if (mgpApplication == null) {
                        mgpApplication = new MgpApplication(serviceRegistry.getSystemName(), appName, version, new ArrayList<>());
                    }
                    MgpInstance mgpInstance = new MgpInstance(instance.getHostName(), appName, instance.getIpAddr(), instance.getPort().get$());
                    mgpApplication.addInstance(mgpInstance);
                }
            }
        }

        return mgpApplication;
    }

    /*public List<MgpApplication> getAppsFromEureka(String systemName, String appName, String version) {
        ServiceRegistry serviceRegistry = serviceRegistryRepository.findBySystemName(systemName);
        List<Instance> registryInstance = instanceRepository.findByServiceRegistryAppId(serviceRegistry.getAppId());
        String registryUrl = "http://" + registryInstance.get(0).getIpAddr() + ":" + registryInstance.get(0).getPort() + "/eureka/apps/" + appName;
        AppList eurekaApp = restTemplate.getForObject(registryUrl, AppList.class);
        List<MgpApplication> mgpApplications = new ArrayList<>();
        for (AppInstance instance: eurekaApp.getApplication().getInstance()) {
            if (instance.getStatus().equals("UP")) {
                String url = "http://" + instance.getIpAddr() + ":" + instance.getPort().get$();
                String ver = getVersionFromRemoteApp(url);
                if (ver.equals(version)) {
                    MgpInstance mgpInstance = new MgpInstance(instance.getHostName(), appName, instance.getIpAddr(), instance.getPort().get$());
                    MgpApplication mgpApplication = new MgpApplication(serviceRegistry.getSystemName(), appName, version, new ArrayList<>());
                    mgpApplication.addInstance(mgpInstance);
                    mgpApplications.add(mgpApplication);
                }
            }
        }

        return mgpApplications;
    }*/

    // Map<appId, Pair<appInfo, number of apps>>
    //public Map<String, Pair<MgpApplication, Integer>> getAppsInfoAndNumFromEurekaAppList(String systemName, AppsList appsList) {
    public Map<String, Pair<MgpApplication, Integer>> getAppsInfoAndNumFromEurekaAppList(String systemName, String eurekaAppId) {
        Map<String, Pair<MgpApplication, Integer>> appInfoAndNum = new HashMap<>();
        AppsList appsList = getAppsListFromEureka(eurekaAppId);
        if (appsList != null) {
            ArrayList<Application> apps = appsList.getApplications().getApplication();
            for (Application app: apps) {
                String appName = app.getName();
                for (AppInstance instance: app.getInstance()) {
                    if (instance.getStatus().equals("UP")) {
                        String url = "http://" + instance.getIpAddr() + ":" + instance.getPort().get$();
                        String version = getVersionFromRemoteApp(url);
                        // Ignore this service if can't get the version.
                        if (version == null) {
                            continue;
                        }
                        String appId = systemName + ":" + appName + ":" + version;
                        Pair<MgpApplication, Integer> appInfo= appInfoAndNum.get(appId);
                        if (appInfo == null) {
                            MgpInstance mgpInstance = new MgpInstance(instance.getHostName(), appName, instance.getIpAddr(), instance.getPort().get$());
                            MgpApplication mgpApplication = new MgpApplication(systemName, appName, version, new ArrayList<>());
                            mgpApplication.addInstance(mgpInstance);
                            appInfo = new MutablePair<>(mgpApplication, 1);
                            appInfoAndNum.put(appId, appInfo);
                        } else {
                            appInfo.setValue(appInfo.getValue() + 1);
                            appInfoAndNum.put(appId, appInfo);
                        }
                    } /*else if (instance.getStatus().equals("DOWN")){
                    // Do something when found a "DOWN" service.
                }*/
                }
            }
        }
        return appInfoAndNum;
    }

    // [SpringFox] // 產生swagger用的  （就是Swagger 2）
    public String getSwaggerFromRemoteApp(String systemName, String appName, String version) {
        MgpApplication mgpApplication = getAppFromEureka(systemName, appName, version);
        String url = "http://" + mgpApplication.getInstances().get(0).getIpAddr() + ":" + mgpApplication.getInstances().get(0).getPort();
        return restTemplate.getForObject(url + "/v2/api-docs", String.class);
    }

    public Map<String, Object> getSwaggerFromRemoteApp(String serviceUrl) {
        String swagger = restTemplate.getForObject(serviceUrl + "/v2/api-docs", String.class);
        Map<String, Object> swaggerMap = null;
        try {
            swaggerMap = mapper.readValue(swagger, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return swaggerMap;
    }

    // [Spring Actuator]

    public String getVersionFromRemoteApp(String serviceUrl) {
        try {
            Info appInfo = restTemplate.getForObject( serviceUrl + ACTUATOR_V1_BASEPATH + "/info", Info.class);
            if (appInfo != null) {
                return appInfo.getVersion();
            } else {
                return null;
            }
        } catch (ResourceAccessException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public List<Trace> getTraceFromRemoteApp(String appUrl) {
        try {
            Trace[] traces = restTemplate.getForObject( appUrl + ACTUATOR_V1_BASEPATH + "/trace", Trace[].class);
            if (traces != null) {
                return Arrays.asList(traces);
            } else {
                return null;
            }
        } catch (ResourceAccessException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public List<Trace> getTracesFromRemoteApp(String systemName, String appName, String version) {
        List<Trace> traces = new ArrayList<>();
        MgpApplication mgpApplication = getAppFromEureka(systemName, appName, version);
        if (mgpApplication != null) {
            for (MgpInstance instance : mgpApplication.getInstances()) {
                String appUrl = "http://" + instance.getIpAddr() + ":" + instance.getPort();
                traces.addAll(getTraceFromRemoteApp(appUrl));
            }
        }
        return traces;
    }
}
