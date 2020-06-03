package com.soselab.microservicegraphplatform.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Ints;
import com.soselab.microservicegraphplatform.bean.mgp.MgpApplication;
import com.soselab.microservicegraphplatform.bean.mgp.MgpInstance;
import com.soselab.microservicegraphplatform.bean.mgp.notification.info.ServiceDownNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.info.ServiceUpNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.warning.DependencyWarningNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.warning.NewerVersionNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.warning.OlderVersionNotification;
import com.soselab.microservicegraphplatform.bean.neo4j.*;
import com.soselab.microservicegraphplatform.bean.eureka.AppsList;
import com.soselab.microservicegraphplatform.bean.neo4j.Queue;
import com.soselab.microservicegraphplatform.controllers.WebPageController;
import com.soselab.microservicegraphplatform.repositories.neo4j.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

@Configuration
public class GraphService {
    private static final Logger logger = LoggerFactory.getLogger(GraphService.class);

    @Autowired
    private GeneralRepository generalRepository;
    @Autowired
    private ServiceRegistryRepository serviceRegistryRepository;
    @Autowired
    private InstanceRepository instanceRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private EndpointRepository endpointRepository;
    @Autowired
    private QueueRepository queueRepository;
    @Autowired
    private WebPageController webPageController;
    @Autowired
    private SpringRestTool springRestTool;
    @Autowired
    private GraphAnalyzer graphAnalyzer;
    @Autowired
    private WebNotificationService notificationService;
    @Autowired
    private MonitorService monitorService;
    @Autowired
    private ContractService contractService;
    @Autowired
    private RiskService riskService;
    @Autowired
    private ObjectMapper mapper;
    private RestTemplate restTemplate = new RestTemplate();

    private Map<String, String> graphJson = new HashMap<>();

    @PostConstruct
    private void init() {
        // Store the graph json of each systems at a Map as cache.
        List<String> systemNames = generalRepository.getAllSystemName();
        for (String systemName : systemNames) {
            contractService.setAllServiceContractTestingCondition(systemName);
            riskService.setServiceRisk(systemName);
            //monitorService.runScheduled(systemName);
            graphJson.put(systemName, generalRepository.getSystemGraphJson(systemName));
        }
    }

    @Scheduled(fixedDelay = 10000) // 10秒跑一次, 更新neo4j圖
    public void run() {
        Map<String, Boolean> systemIsUpdatedMap = updateGraphDB();
        systemIsUpdatedMap.forEach((systemName, isUpdated) -> {
            if (isUpdated) {
                contractService.setAllServiceContractTestingCondition(systemName);
                riskService.setServiceRisk(systemName);
                monitorService.runScheduled(systemName);
                updateGraphJson(systemName);
            }
        });
    }

    // 視覺化時要把NEO4j的資料提取成JSON, 此function更改那個json
    private void updateGraphJson(String systemName) {
        graphJson.remove(systemName);
        graphJson.put(systemName, generalRepository.getSystemGraphJson(systemName)); // 從neo4j拿
//        graphJson.replace(systemName, generalRepository.getSystemGraphJson(systemName)); // 從neo4j拿
        webPageController.sendGraph(systemName, graphJson.get(systemName)); // push 到VMAMVS前端
    }

    // 拿服務暫存的json, （webController用）
    public String getGraphJson(String systemName) {
        return graphJson.get(systemName);
    }

    // 更新neo4j的資料
    private Map<String, Boolean> updateGraphDB() {
        Map<String, Boolean> updated = new HashMap<>();
        // For each service registry
        ArrayList<ServiceRegistry> registries = serviceRegistryRepository.findAll();
        if (registries.size() > 0) {
            for (ServiceRegistry serviceRegistry : registries) { //分類服務, （上線 下線 重新上線）
                // Get latest app list by request the first instance that own by this service registry
                String systemName = serviceRegistry.getSystemName();
                Map<String, Pair<MgpApplication, Integer>> eurekaAppsInfoAndNum =
                        springRestTool.getAppsInfoAndNumFromEurekaAppList(systemName, serviceRegistry.getAppId());
                List<Service> ServicesInDB = serviceRepository.findBySysName(serviceRegistry.getSystemName());
                List<NullService> nullServiceInDB = serviceRepository.findNullBySysName(serviceRegistry.getSystemName());
                // Check the service should be created or updated or removed in graph DB.
                Map<String, Pair<MgpApplication, Integer>> newAppsMap = new HashMap<>();
                Map<String, Pair<MgpApplication, Integer>> recoveryAppsMap = new HashMap<>();
                Map<String, Pair<MgpApplication, Integer>> updateAppsMap;
                Set<String> removeAppsSet = new HashSet<>();
                // Find new apps and recover apps, then remove from eurekaAppsInfoAndNum.
                for (Iterator<Map.Entry<String, Pair<MgpApplication, Integer>>> it = eurekaAppsInfoAndNum.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, Pair<MgpApplication, Integer>> entry = it.next();
                    boolean isUpInDB = false;
                    boolean isNull = false;
                    for (Service dbApp : ServicesInDB) {
                        if (entry.getKey().equals(dbApp.getAppId())) {
                            isUpInDB = true;
                            break;
                        }
                    }
                    for (NullService nullDbApp: nullServiceInDB) {
                        if (entry.getKey().equals(nullDbApp.getAppId())) {
                            isNull = true;
                            break;
                        }
                    }
                    if (!isUpInDB) {
                        if (isNull) {
                            recoveryAppsMap.put(entry.getKey(), entry.getValue());
                        } else {
                            newAppsMap.put(entry.getKey(), entry.getValue());
                        }
                        it.remove();
                    }
                }
                // The remaining apps in eurekaAppsInfoAndNum should be updated.
                updateAppsMap = eurekaAppsInfoAndNum;
                // Find apps not in eurekaAppsInfoAndNum.
                for (Service dbApp : ServicesInDB) {
                    boolean isInRefreshList = false;
                    for (Map.Entry<String, Pair<MgpApplication, Integer>> entry: eurekaAppsInfoAndNum.entrySet()) {
                        if (dbApp.getAppId().equals(entry.getKey())) {
                            isInRefreshList = true;
                            break;
                        }
                    }
                    if (!isInRefreshList) {
                        removeAppsSet.add(dbApp.getAppId());
                    }
                }

                // Update the dependency graph.
                Map<String, Map<String, Object>> appSwaggers = addServices(serviceRegistry, newAppsMap);
                appSwaggers.putAll(recoverServices(serviceRegistry, recoveryAppsMap));

                newAppsMap.putAll(recoveryAppsMap);
                addDependencies(newAppsMap, appSwaggers);

                boolean appsUpdated = updateServices(updateAppsMap);
                removeServices(systemName, removeAppsSet);

                // If the graph was be updated then return true.
                if (newAppsMap.size() > 0 || removeAppsSet.size() > 0 || appsUpdated) {
                    // App up notification.
                    if (newAppsMap.size() > 0) {
                        for (Map.Entry<String, Pair<MgpApplication, Integer>> entry: newAppsMap.entrySet()) {
                            String appName = entry.getValue().getKey().getAppName();
                            String version = entry.getValue().getKey().getVersion();
                            notificationService.pushNotificationToSystem(systemName, new ServiceUpNotification(appName, version));
                        }
                    }
                    // App down notification.
                    if (removeAppsSet.size() > 0) {
                        for (String appId : removeAppsSet) {
                            String appName = appId.split(":")[1];
                            String version = appId.split(":")[2];
                            notificationService.pushNotificationToSystem(systemName, new ServiceDownNotification(appName, version));
                        }
                    }

                    graphAnalyzer.checkDependenciesOfAppsInSystem(systemName);

                    updated.put(systemName, true);
                } else {
                    updated.put(systemName, false);
                }
                monitorService.runScheduled(systemName);
            }
        }

        return updated;
    }

    // Add new apps to neo4j
    private Map<String, Map<String, Object>> addServices(ServiceRegistry serviceRegistry, Map<String, Pair<MgpApplication, Integer>> newAppsMap) {
        Map<String, Map<String, Object>> appSwaggers = new HashMap<>();
        Map<String, Pair<MgpApplication, Integer>> updateDependencyAppsMap = new HashMap<>();
        Map<String, Map<String, Object>> updateDependencyAppsSwagger = new HashMap<>();
        // Add services to graph DB.
        newAppsMap.forEach((appId, appInfoAndNum) -> {
            MgpInstance instance = appInfoAndNum.getKey().getInstances().get(0);
            String serviceUrl = "http://" + instance.getIpAddr() + ":" + instance.getPort();
            Map<String, Object> swaggerMap = springRestTool.getSwaggerFromRemoteApp(serviceUrl);
            if (swaggerMap != null) {
                appSwaggers.put(appId, swaggerMap);
                MgpApplication app = appInfoAndNum.getKey();
                Service service = new Service(app.getSystemName(), app.getAppName(), app.getVersion(), appInfoAndNum.getValue());
                service.registerTo(serviceRegistry);
                Map<String, Object> pathsMap = mapper.convertValue(swaggerMap.get("paths"), new TypeReference<Map<String, Object>>(){});
                pathsMap.forEach((pathKey, pathValue) -> {
                    Map<String, Object> methodMap = mapper.convertValue(pathValue, new TypeReference<Map<String, Object>>(){});
                    methodMap.forEach((methodKey, methodValue) -> {
                        Endpoint endpoint = new Endpoint(serviceRegistry.getSystemName(), app.getAppName(), methodKey, pathKey);
                        service.ownEndpoint(endpoint);
                    });
                });
                // Find newer patch version service.
                Service newerPatchService = graphAnalyzer.newerPatchVersionDetector(appInfoAndNum.getKey());
                if (newerPatchService != null) {
                    service.addLabel("OutdatedVersion");
                    service.foundNewPatchVersion(newerPatchService);
                } else {
                    List<Service> olderPatchServices = graphAnalyzer.olderPatchVersionDetector(appInfoAndNum.getKey());
                    for (Service olderPatchService : olderPatchServices) {
                        serviceRepository.addOutdatedVersionLabelAndDeleteNewrPatchVerRelByAppId(olderPatchService.getAppId());
                    }
                    service.foundOldPatchVersions(olderPatchServices);
                }
                serviceRepository.save(service);
                logger.info("Add service: " + service.getAppId());
            }
            List<Pair<MgpApplication, Map<String, Object>>> dependencyDetectResult = graphAnalyzer.dependencyDetector(serviceRegistry, appInfoAndNum.getKey());
            for (Pair<MgpApplication, Map<String, Object>> app : dependencyDetectResult) {
                updateDependencyAppsMap.put(app.getKey().getAppId(), new MutablePair<>(app.getKey(), null));
                updateDependencyAppsSwagger.put(app.getKey().getAppId(), app.getValue());
            }
            updateDependencies(updateDependencyAppsMap, updateDependencyAppsSwagger);
        });

        return appSwaggers;
    }

    // Add dependency relationships to neo4j （delete不會跑這個）
    private void updateDependencies(Map<String, Pair<MgpApplication, Integer>> appsMap, Map<String, Map<String, Object>> appSwaggers) {
        appsMap.forEach((appId, appInfo) -> {
            serviceRepository.deleteDependencyByAppId(appInfo.getKey().getAppId());
        });
        if (appsMap.size() > 0) {
            endpointRepository.deleteUselessNullEndpoint();
            serviceRepository.deleteUselessNullService();
        }
        addDependencies(appsMap, appSwaggers);
    }

    // Recover apps in neo4j （已經確定服務有上線, Eureka Server 註冊表, ）
    private Map<String, Map<String, Object>> recoverServices(ServiceRegistry serviceRegistry, Map<String, Pair<MgpApplication, Integer>> recoveryAppsMap) {
        Map<String, Map<String, Object>> appSwaggers = new HashMap<>();
        // Add services to graph DB.
        // CASE: Replace services
        recoveryAppsMap.forEach((appId, appInfoAndNum) -> {
            MgpInstance instance = appInfoAndNum.getKey().getInstances().get(0);
            String serviceUrl = "http://" + instance.getIpAddr() + ":" + instance.getPort();
            Map<String, Object> swaggerMap = springRestTool.getSwaggerFromRemoteApp(serviceUrl);
            if (swaggerMap != null) {
                appSwaggers.put(appId, swaggerMap);
                //String noVerAppId = appInfoAndNum.getKey().getSystemName() + ":" + appInfoAndNum.getKey().getAppName() + ":null";
                //if (!serviceRepository.removeNullLabelAndSetVerAndNumByAppId(noVerAppId, appInfoAndNum.getKey().getAppId(),
                //        appInfoAndNum.getKey().getVersion(), appInfoAndNum.getValue())) {
                    serviceRepository.removeNullLabelAndSetNumByAppId(appId, appInfoAndNum.getValue());
                //}
                List<Endpoint> nullEndpoints = endpointRepository.findByAppId(appId);
                List<Endpoint> newEndpoints = new ArrayList<>();
                Map<String, Object> pathsMap = mapper.convertValue(swaggerMap.get("paths"), new TypeReference<Map<String, Object>>(){});
                pathsMap.forEach((path, pathValue) -> {
                    Map<String, Object> methodMap = mapper.convertValue(pathValue, new TypeReference<Map<String, Object>>(){});
                    methodMap.forEach((method, methodValue) -> {
                        boolean recoveryEndpoint = false;
                        for (Endpoint nullEndpoint: nullEndpoints) {
                            if (path.equals(nullEndpoint.getPath()) && method.equals(nullEndpoint.getMethod())) {
                                endpointRepository.removeNullLabelByAppIdAAndEndpointId(appId, method + ":" + path);
                                recoveryEndpoint = true;
                                break;
                            }
                        }
                        if (!recoveryEndpoint) {
                            Endpoint endpoint = new Endpoint(serviceRegistry.getSystemName(), appInfoAndNum.getKey().getAppName(), method, path);
                            newEndpoints.add(endpoint);
                        }
                    });
                });
                Service service = serviceRepository.findByAppId(appId);
                service.registerTo(serviceRegistry);
                service.ownEndpoint(newEndpoints);
                serviceRepository.save(service);
                logger.info("Recover service: " + service.getAppId());
            }
            graphAnalyzer.newerPatchVersionDetector(appInfoAndNum.getKey());
        });

        return appSwaggers;
    }

    // Add dependency relationships to neo4j
    private void addDependencies(Map<String, Pair<MgpApplication, Integer>> appsMap, Map<String, Map<String, Object>> appSwaggers) {
        appsMap.forEach((sourceAppId, sourceAppInfo) -> {
            Map<String, Object> swaggerMap = appSwaggers.get(sourceAppId);
            if (swaggerMap != null) {
                Map<String, Object> dependencyMap = mapper.convertValue(swaggerMap.get("x-serviceDependency"), new TypeReference<Map<String, Object>>(){});
                if (dependencyMap.get("httpRequest") != null) {
                    Map<String, Object> sourcePathMap = mapper.convertValue(dependencyMap.get("httpRequest"), new TypeReference<Map<String, Object>>(){});
                    sourcePathMap.forEach((sourcePath, sourcePathValue) -> {
                        if (sourcePath.equals("none")) {
                            Service sourceService = serviceRepository.findByAppId(sourceAppId);
                            Object targets = mapper.convertValue(sourcePathValue, Map.class).get("targets");
                            addHttpTargetEndpoints(sourceService, mapper.convertValue(targets, new TypeReference<Map<String, Object>>(){}));
                        } else {
                            Map<String, Object> sourceMethodMap = mapper.convertValue(sourcePathValue, new TypeReference<Map<String, Object>>(){});
                            sourceMethodMap.forEach((sourceMethodKey, sourceMethodValue) -> {
                                String sourceEndpointId = sourceMethodKey + ":" + sourcePath;
                                Endpoint sourceEndpoint = endpointRepository.findByEndpointIdAndAppId(sourceEndpointId, sourceAppId);
                                Object targets = mapper.convertValue(sourceMethodValue, Map.class).get("targets");
                                addHttpTargetEndpoints(sourceAppId, sourceEndpoint, mapper.convertValue(targets, new TypeReference<Map<String, Object>>(){}));
                            });
                        }
                    });
                }
                if (dependencyMap.get("amqp") != null) {
                    Map<String, Object> typeMap = mapper.convertValue(dependencyMap.get("amqp"), new TypeReference<Map<String, Object>>(){});
                    if (typeMap.get("publish") != null) {
                        Map<String, Object> publishMap = mapper.convertValue(typeMap.get("publish"), new TypeReference<Map<String, Object>>(){});
                        publishMap.forEach((sourcePath, sourcePathValue) -> {
                            if (sourcePath.equals("none")) {
                                Service sourceService = serviceRepository.findByAppId(sourceAppId);
                                Map<String, List<String>> sourceTargetMap = mapper.convertValue(sourcePathValue, new TypeReference<Map<String, ArrayList<String>>>(){});
                                addAmqpPublishQueue(sourceService, sourceTargetMap.get("targets"));
                            } else {
                                Map<String, Object> sourceMethodMap = mapper.convertValue(sourcePathValue, new TypeReference<Map<String, Object>>(){});
                                sourceMethodMap.forEach((sourceMethod, sourceMethodValue) -> {
                                    String sourceEndpointId = sourceMethod + ":" + sourcePath;
                                    Endpoint sourceEndpoint = endpointRepository.findByEndpointIdAndAppId(sourceEndpointId, sourceAppId);
                                    Map<String, List<String>> sourceTargetMap = mapper.convertValue(sourceMethodValue, new TypeReference<Map<String, ArrayList<String>>>(){});
                                    addAmqpPublishQueue(sourceAppInfo.getKey().getSystemName(), sourceEndpoint, sourceTargetMap.get("targets"));
                                });
                            }
                        });
                    }
                    if (typeMap.get("subscribe") != null) {
                        Map<String, Object> subscribeMap = mapper.convertValue(typeMap.get("subscribe"), new TypeReference<Map<String, Object>>(){});
                        subscribeMap.forEach((sourcePath, sourcePathValue) -> {
                            if (sourcePath.equals("none")) {
                                Service sourceService = serviceRepository.findByAppId(sourceAppId);
                                Map<String, List<String>> sourceTargetMap = mapper.convertValue(sourcePathValue, new TypeReference<Map<String, ArrayList<String>>>(){});
                                addAmqpSubscribeQueue(sourceService, sourceTargetMap.get("targets"));
                            } else {
                                Map<String, Object> sourceMethodMap = mapper.convertValue(sourcePathValue, new TypeReference<Map<String, Object>>(){});
                                sourceMethodMap.forEach((sourceMethod, sourceMethodValue) -> {
                                    String sourceEndpointId = sourceMethod + ":" + sourcePath;
                                    Endpoint sourceEndpoint = endpointRepository.findByEndpointIdAndAppId(sourceEndpointId, sourceAppId);
                                    Map<String, List<String>> sourceTargetMap = mapper.convertValue(sourceMethodValue, new TypeReference<Map<String, ArrayList<String>>>(){});
                                    addAmqpSubscribeQueue(sourceAppInfo.getKey().getSystemName(), sourceEndpoint, sourceTargetMap.get("targets"));
                                });
                            }
                        });
                    }
                }
            }
        });
    }

    private void addHttpTargetEndpoints(Service sourceService, Map<String, Object> targets) {
        Set<Endpoint> targetEndpoints = getHttpTargetEndpoints(sourceService.getAppId(), targets);
        for (Endpoint targetEndpoint: targetEndpoints) {
            sourceService.httpRequestToEndpoint(targetEndpoint);
        }
        serviceRepository.save(sourceService);
    }

    private void addHttpTargetEndpoints(String sourceAppId, Endpoint sourceEndpoint, Map<String, Object> targets) {
        Set<Endpoint> targetEndpoints = getHttpTargetEndpoints(sourceAppId, targets);
        for (Endpoint targetEndpoint: targetEndpoints) {
            sourceEndpoint.httpRequestToEndpoint(targetEndpoint);
        }
        endpointRepository.save(sourceEndpoint);
    }

    private Set<Endpoint> getHttpTargetEndpoints(String sourceAppId, Map<String, Object> targets) {
        Set<Endpoint> targetEndpoints = new HashSet<>();
        targets.forEach((targetServiceKey, targetServiceValue) -> {
            String targetServiceName = targetServiceKey.toUpperCase();
            Map<String, Object> targetVersionMap =
                    mapper.convertValue(targetServiceValue, new TypeReference<Map<String, Object>>(){});
            targetVersionMap.forEach((targetVersionKey, targetVersionValue) -> {
                Map<String, Object> targetPathMap =
                        mapper.convertValue(targetVersionValue, new TypeReference<Map<String, Object>>(){});
                targetPathMap.forEach((targetPathKey, targetPathValue) -> {
                    ArrayList<String> targetMethods = (ArrayList<String>) targetPathValue;
                    for (String targetMethod: targetMethods) {
                        String targetEndpointId =  targetMethod + ":" + targetPathKey;
                        if (targetVersionKey.equals("notSpecified")) {
                            List<Endpoint> targetEndpoint = endpointRepository.findTargetEndpointNotSpecVer(
                                    sourceAppId, targetServiceName, targetEndpointId
                            );
                            targetEndpoints.addAll(graphAnalyzer.nullHttpTargetDetector(targetEndpoint, sourceAppId, targetServiceName,
                                    targetMethod, targetPathKey));
                        } else {
                            Endpoint targetEndpoint = endpointRepository.findTargetEndpoint(
                                    sourceAppId, targetServiceName, targetVersionKey,
                                    targetEndpointId);
                            targetEndpoints.add(graphAnalyzer.nullHttpTargetDetector(targetEndpoint, sourceAppId, targetServiceName,
                                    targetMethod, targetPathKey, targetVersionKey));
                        }
                    }
                });
            });
        });

        return targetEndpoints;
    }

    private void addAmqpPublishQueue(Service sourceService, List<String> targets) {
        List<Queue> queues = new ArrayList<>();
        for (String queueName : targets) {
            String queueId = sourceService.getSystemName() + ":" + queueName;
            Queue queue = queueRepository.findByQueueId(queueId);
            if (queue != null) {
                queues.add(queue);
            } else {
                queues.add(new Queue(sourceService.getSystemName(), queueName));
            }
        }
        sourceService.amqpPublishToQueue(queues);
        serviceRepository.save(sourceService);
    }

    private void addAmqpPublishQueue(String sysName, Endpoint endpoint, List<String> targets) {
        List<Queue> queues = new ArrayList<>();
        for (String queueName : targets) {
            String queueId = sysName + ":" + queueName;
            Queue queue = queueRepository.findByQueueId(queueId);
            if (queue != null) {
                queues.add(queue);
            } else {
                queues.add(new Queue(sysName, queueName));
            }
        }
        endpoint.amqpPublishToQueue(queues);
        endpointRepository.save(endpoint);
    }

    private void addAmqpSubscribeQueue(Service sourceService, List<String> targets) {
        List<Queue> queues = new ArrayList<>();
        for (String queueName : targets) {
            String queueId = sourceService.getSystemName() + ":" + queueName;
            Queue queue = queueRepository.findByQueueId(queueId);
            if (queue != null) {
                queues.add(queue);
            } else {
                queues.add(new Queue(sourceService.getSystemName(), queueName));
            }
        }
        sourceService.amqpSubscribeToQueue(queues);
        serviceRepository.save(sourceService);
    }

    private void addAmqpSubscribeQueue(String sysName, Endpoint endpoint, List<String> targets) {
        List<Queue> queues = new ArrayList<>();
        for (String queueName : targets) {
            String queueId = sysName + ":" + queueName;
            Queue queue = queueRepository.findByQueueId(queueId);
            if (queue != null) {
                queues.add(queue);
            } else {
                queues.add(new Queue(sysName, queueName));
            }
        }
        endpoint.amqpSubscribeToQueue(queues);
        endpointRepository.save(endpoint);
    }

    // 此function更新服務的實例數量（方塊裡面的數字）
    private boolean updateServices(Map<String, Pair<MgpApplication, Integer>> updateAppsMap) {
        boolean updated = false;
        for (Map.Entry<String, Pair<MgpApplication, Integer>> entry: updateAppsMap.entrySet()) {
            if (!serviceRepository.setNumberByAppId(entry.getKey(), entry.getValue().getValue())){
                updated = true;
            }
        }

        return updated;
    }

    // Remove apps that are not in eureka's app list
    private void removeServices(String systemName, Set<String> removeAppsSet) {
        for (String appId : removeAppsSet) {
            if (serviceRepository.isBeDependentByAppId(appId)) {
                logger.info("Found null service: " + appId);
                serviceRepository.setNumToZeroAndAddNullLabelWithEndpointsByAppId(appId);
            } else {
                serviceRepository.deleteWithEndpointsAndSettingByAppId(appId);
            }
            logger.info("Remove service: " + appId);
        }

        if (removeAppsSet.size() > 0) {
            endpointRepository.deleteUselessNullEndpoint();
            serviceRepository.deleteUselessNullService();
            queueRepository.deleteUselessQueues();
            serviceRepository.removeUselessOutdatedVersionLabel();
        }
    }

    public void appSettingUpdatedEvent(String appId) {
        Service service = serviceRepository.findByAppIdWithSettingNotNull(appId);
        if (service != null && graphAnalyzer.checkDependenciesOfApp(service)) {
            updateGraphJson(service.getSystemName());
        }
    }

}
