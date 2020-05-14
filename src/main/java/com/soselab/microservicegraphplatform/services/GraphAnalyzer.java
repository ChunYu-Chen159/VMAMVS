package com.soselab.microservicegraphplatform.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Ints;
import com.soselab.microservicegraphplatform.bean.mgp.MgpApplication;
import com.soselab.microservicegraphplatform.bean.mgp.notification.warning.DependencyWarningNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.warning.NewerVersionNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.warning.OlderVersionNotification;
import com.soselab.microservicegraphplatform.bean.neo4j.*;
import com.soselab.microservicegraphplatform.repositories.neo4j.EndpointRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.GeneralRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class GraphAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(GraphAnalyzer.class);

    @Autowired
    private GeneralRepository generalRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private EndpointRepository endpointRepository;
    @Autowired
    private SpringRestTool springRestTool;
    @Autowired
    private WebNotificationService notificationService;
    @Autowired
    private ObjectMapper mapper;

    // Find services that needs to update dependencies caused by an input service and
    // List<Pair<App info, Swagger>> Apps
    public List<Pair<MgpApplication, Map<String, Object>>> dependencyDetector(ServiceRegistry serviceRegistry, MgpApplication mgpApplication) {
        List<Pair<MgpApplication, Map<String, Object>>> updateDependencyApps = new ArrayList<>();
        Service noVerNullApp = serviceRepository.findNullByAppId(mgpApplication.getSystemName() + ":" + mgpApplication.getAppName() + ":null");
        if (noVerNullApp != null) {
            List<Service> dependentApps = serviceRepository.findDependentOnThisAppByAppId(noVerNullApp.getAppId());
            for (Service dependentApp : dependentApps) {
                MgpApplication appInfo = springRestTool.getAppFromEureka
                        (serviceRegistry, dependentApp.getAppName(), dependentApp.getVersion());
                String ipAddr = appInfo.getInstances().get(0).getIpAddr();
                int port = appInfo.getInstances().get(0).getPort();
                String serviceUrl = "http://" + ipAddr + ":" + port;
                Map<String, Object> swaggerMap = springRestTool.getSwaggerFromRemoteApp(serviceUrl);
                updateDependencyApps.add(new MutablePair<>(appInfo, swaggerMap));
            }
        } else {
            Service otherVerApp = serviceRepository.findOtherVerInSameSysBySysNameAndAppNameAndVersion
                    (mgpApplication.getSystemName(), mgpApplication.getAppName(), mgpApplication.getVersion());
            if (otherVerApp != null) {
                List<Service> dependentApps = serviceRepository.findDependentOnThisAppByAppId(otherVerApp.getAppId());
                if (dependentApps != null) {
                    for (Service dependentApp : dependentApps) {
                        MgpApplication appInfo = springRestTool.getAppFromEureka
                                (serviceRegistry, dependentApp.getAppName(), dependentApp.getVersion());
                        String ipAddr = appInfo.getInstances().get(0).getIpAddr();
                        int port = appInfo.getInstances().get(0).getPort();
                        String serviceUrl = "http://" + ipAddr + ":" + port;
                        Map<String, Object> swaggerMap = springRestTool.getSwaggerFromRemoteApp(serviceUrl);
                        if (swaggerMap != null) {
                            Map<String, Object> dependencyMap = mapper.convertValue(swaggerMap.get("x-serviceDependency"), new TypeReference<Map<String, Object>>(){});
                            if (dependencyMap.get("httpRequest") != null) {
                                Map<String, Object> sourcePathMap = mapper.convertValue(dependencyMap.get("httpRequest"), new TypeReference<Map<String, Object>>(){});
                                sourcePathMap.forEach((sourcePathKey, sourcePathValue) -> {
                                    Map<String, Object> targetsMap = new HashMap<>();
                                    if (sourcePathKey.equals("none")) {
                                        Object targets = mapper.convertValue(sourcePathValue, Map.class).get("targets");
                                        targetsMap = mapper.convertValue(targets, new TypeReference<Map<String, Object>>(){});
                                    } else {
                                        Map<String, Object> sourceMethodMap = mapper.convertValue(sourcePathValue, new TypeReference<Map<String, Object>>(){});
                                        for (Map.Entry<String, Object> methodEntry : sourceMethodMap.entrySet()) {
                                            Object targets = mapper.convertValue(methodEntry.getValue(), Map.class).get("targets");
                                            targetsMap = mapper.convertValue(targets, new TypeReference<Map<String, Object>>(){});
                                        }
                                    }
                                    if (isTargetsExistNotSpecifiedCalltoApp(mgpApplication.getAppName(), targetsMap)) {
                                        updateDependencyApps.add(new MutablePair<>(appInfo, swaggerMap));
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }



        return updateDependencyApps;
    }

    // Whether the version of service was assigned
    private boolean isTargetsExistNotSpecifiedCalltoApp(String targetAppName, Map<String, Object> targets) {
        boolean result = false;
        // key = targetName
        for (Map.Entry<String, Object> targetEntry : targets.entrySet()) {
            if (targetEntry.getKey().toUpperCase().equals(targetAppName)) {
                Map<String, Object> targetVersionMap =
                        mapper.convertValue(targetEntry.getValue(), new TypeReference<Map<String, Object>>(){});
                // key = version
                for (Map.Entry<String, Object> versionEntry : targetVersionMap.entrySet()) {
                    if (versionEntry.getKey().equals("notSpecified")) {
                        result = true;
                        break;
                    }
                }
                break;
            }
        }

        return result;
    }

    // 0.0.1-SNAPSHOT , thirth number 0.0.2 is newer   (semantic versioning)
    public Service newerPatchVersionDetector(MgpApplication mgpApplication) {
        int[] thisAppVerCode = getVersionCode(mgpApplication.getVersion());
        if (thisAppVerCode != null) {
            List<Service> otherVerServices = serviceRepository.findOtherVersInSameSysBySysNameAndAppNameAndVersion
                    (mgpApplication.getSystemName(), mgpApplication.getAppName(), mgpApplication.getVersion());
            if (otherVerServices.size() > 0) {
                Pair<Service, int[]> latestPathApp = new ImmutablePair<>(null, thisAppVerCode);
                for (Service otherVerService : otherVerServices) {
                    int[] otherAppVerCode = getVersionCode(otherVerService.getVersion());
                    if (otherAppVerCode != null) {
                        if (thisAppVerCode[0] == otherAppVerCode [0] && thisAppVerCode[1] == otherAppVerCode[1]) {
                            if (otherAppVerCode[2] > latestPathApp.getValue()[2]) {
                                latestPathApp = new ImmutablePair<>(otherVerService, otherAppVerCode);
                            }
                        }
                    }
                }
                if (latestPathApp.getKey() != null) {
                    notificationService.pushNotificationToSystem(mgpApplication.getSystemName(),
                            new NewerVersionNotification(mgpApplication.getAppName(), mgpApplication.getVersion(),
                                    latestPathApp.getKey().getVersion(), NewerVersionNotification.LEVEL_PATCH));
                    logger.info("Found newer patch version: " + mgpApplication.getAppId() + " -> " + latestPathApp.getKey().getVersion());
                    return latestPathApp.getKey();
                }
            }
        }
        return null;
    }

    // 0.0.2-SNAPSHOT , thirth number 0.0.1 is older (semantic versioning)
    public List<Service> olderPatchVersionDetector(MgpApplication mgpApplication) {
        int[] thisAppVerCode = getVersionCode(mgpApplication.getVersion());
        List<Service> olderVerServices = new ArrayList<>();
        if (thisAppVerCode != null) {
            List<Service> otherVerServices = serviceRepository.findOtherVersInSameSysBySysNameAndAppNameAndVersion
                    (mgpApplication.getSystemName(), mgpApplication.getAppName(), mgpApplication.getVersion());
            if (otherVerServices.size() > 0) {
                for (Service otherVerService : otherVerServices) {
                    int[] otherAppVerCode = getVersionCode(otherVerService.getVersion());
                    if (otherAppVerCode != null) {
                        if (thisAppVerCode[0] == otherAppVerCode [0] && thisAppVerCode[1] == otherAppVerCode[1]) {
                            if (otherAppVerCode[2] < thisAppVerCode[2]) {
                                olderVerServices.add(otherVerService);
                                notificationService.pushNotificationToSystem(mgpApplication.getSystemName(),
                                        new OlderVersionNotification(mgpApplication.getAppName(), mgpApplication.getVersion(),
                                                otherVerService.getVersion(), OlderVersionNotification.LEVEL_PATCH));
                                logger.info("Found older patch version: " + mgpApplication.getAppId() + " -> " + otherVerService.getVersion());
                            }
                        }
                    }
                }
            }
        }
        return olderVerServices;
    }

    // Get an version number array form a semantic versioning string.
    private int[] getVersionCode(String version) {
        String[] versionParts = version.split("\\.");
        if (versionParts.length >= 3) {
            List<Integer> versionCode = new ArrayList<>();
            for (String part : versionParts) {
                if (versionCode.size() == 0 && String.valueOf(part.charAt(part.length() - 1)).matches("\\d")) {
                    String[] numsInPart = part.split("\\D+");
                    versionCode.add(Integer.parseInt(numsInPart[numsInPart.length - 1]));
                } else if (versionCode.size() == 1){
                    if (part.matches("\\d+")) {
                        versionCode.add(Integer.parseInt(part));
                    } else {
                        versionCode.clear();
                    }
                } else if (versionCode.size() == 2) {
                    if (String.valueOf(part.charAt(0)).matches("\\d")) {
                        String[] numsInPart = part.split("\\D+");
                        versionCode.add(Integer.parseInt(numsInPart[0]));
                    } else {
                        versionCode.clear();
                    }
                } else {
                    if (versionCode.size() == 3) {
                        break;
                    }
                }
            }
            if (versionCode.size() == 3) {
                return Ints.toArray(versionCode);
            }
        }
        return null;
    }

    // whether null point is exising (List<Endpoint>) (square)
    public List<Endpoint> nullHttpTargetDetector(List<Endpoint> targetEndpoints, String sourceAppId, String targetName,
                                                  String targetMethod, String targetPath) {
        if (targetEndpoints != null && targetEndpoints.size() > 0) {
            // Normal situation
            return targetEndpoints;
        } else {
            List<Endpoint> nullEndpoints = new ArrayList<>();
            List<Service> targetServices = serviceRepository.findByAppNameInSameSys(
                    sourceAppId, targetName);
            if (targetServices != null && targetServices.size() > 0) {
                // Found null target endpoints.
                for (Service targetService: targetServices) {
                    //Endpoint nullTargetEndpoint = new NullEndpoint(targetName, targetMethod, targetPath);
                    Endpoint nullTargetEndpoint = endpointRepository.findByNullEndpointAndAppId(targetMethod + ":" + targetPath, targetService.getAppId());
                    if (nullTargetEndpoint == null) {
                        nullTargetEndpoint = new NullEndpoint(sourceAppId.split(":")[0], targetName, targetMethod, targetPath);
                        nullTargetEndpoint.ownBy(targetService);
                        endpointRepository.save(nullTargetEndpoint);
                        nullTargetEndpoint = endpointRepository.findByNullEndpointAndAppId(nullTargetEndpoint.getEndpointId(), targetService.getAppId());
                    }
                    nullEndpoints.add(nullTargetEndpoint);
                    logger.info("Found null endpoint of service: " + targetService.getAppId() + " " + nullTargetEndpoint.getEndpointId());
                }
            } else {
                // Found null target service and endpoint.
                Service nullTargetService = serviceRepository.findNullByAppId(sourceAppId.split(":")[0] + ":" + targetName + ":" + null);
                if (nullTargetService == null) {
                    nullTargetService = new NullService(
                            sourceAppId.split(":")[0], targetName, null, 0);
                    Endpoint nullTargetEndpoint = new NullEndpoint(sourceAppId.split(":")[0], targetName, targetMethod, targetPath);
                    nullTargetEndpoint.ownBy(nullTargetService);
                    endpointRepository.save(nullTargetEndpoint);
                    nullTargetEndpoint = endpointRepository.findByNullEndpointAndAppId(nullTargetEndpoint.getEndpointId(), nullTargetService.getAppId());
                    nullEndpoints.add(nullTargetEndpoint);
                    logger.info("Found null service and endpoint: " + nullTargetService.getAppId() + " " + nullTargetEndpoint.getEndpointId());
                } else {
                    Endpoint nullTargetEndpoint = endpointRepository.findByNullEndpointAndAppId(targetMethod + ":" + targetPath, nullTargetService.getAppId());
                    if (nullTargetEndpoint == null) {
                        nullTargetEndpoint = new NullEndpoint(sourceAppId.split(":")[0], targetName, targetMethod, targetPath);
                        nullTargetEndpoint.ownBy(nullTargetService);
                        endpointRepository.save(nullTargetEndpoint);
                        nullTargetEndpoint = endpointRepository.findByNullEndpointAndAppId(nullTargetEndpoint.getEndpointId(), nullTargetService.getAppId());
                    }
                    nullEndpoints.add(nullTargetEndpoint);
                    logger.info("Found null service and endpoint: " + nullTargetService.getAppId() + " " + nullTargetEndpoint.getEndpointId());
                }
            }
            return nullEndpoints;
        }
    }

    // whether null point is exising (Endpoint) (small circle in graph)
    public Endpoint nullHttpTargetDetector(Endpoint targetEndpoint, String sourceAppId, String targetName,
                                            String targetMethod, String targetPath, String targetVersion) {
        if (targetEndpoint != null) {
            // Normal situation
            return targetEndpoint;
        } else {
            String targetAppId = sourceAppId.split(":")[0] + ":" + targetName + ":" + targetVersion;
            String targetEndpointId = targetMethod + ":" + targetPath;
            Service targetService = serviceRepository.findByAppId(targetAppId);
            Endpoint nullTargetEndpoint;
            if (targetService != null) {
                // Found null target endpoint.
                nullTargetEndpoint = endpointRepository.findByNullEndpointAndAppId(targetEndpointId, targetService.getAppId());
                if (nullTargetEndpoint == null) {
                    nullTargetEndpoint = new NullEndpoint(sourceAppId.split(":")[0], targetName, targetMethod, targetPath);
                    nullTargetEndpoint.ownBy(targetService);
                    endpointRepository.save(nullTargetEndpoint);
                    nullTargetEndpoint = endpointRepository.findByNullEndpointAndAppId(targetEndpointId, targetService.getAppId());
                }
                logger.info("Found null endpoint: " + nullTargetEndpoint.getEndpointId());
            } else {
                // Found null target service and endpoint.
                Service nullTargetService = new NullService(
                        sourceAppId.split(":")[0], targetName, targetVersion, 0
                );
                // Find newer patch version service.
                Service newerPatchService = newerPatchVersionDetector
                        (new MgpApplication(sourceAppId.split(":")[0], targetName, targetVersion, null));
                if (newerPatchService != null) {
                    nullTargetService.addLabel("OutdatedVersion");
                    nullTargetService.foundNewPatchVersion(newerPatchService);
                }
                nullTargetEndpoint = new NullEndpoint(sourceAppId.split(":")[0], targetName, targetMethod, targetPath);
                nullTargetEndpoint.ownBy(nullTargetService);
                endpointRepository.save(nullTargetEndpoint);
                logger.info("Found null service and endpoint: " + nullTargetService.getAppId() + " " + nullTargetEndpoint.getEndpointId());
            }
            return nullTargetEndpoint;
        }
    }

    // 系統中是否有存在依賴問題 （Strong upper dependency chain, Weak upper dependency chain）
    // （圖更新時會跑一次檢查） 之後修改成Strict 和 loose
    public void checkDependenciesOfAppsInSystem(String systemName) {
        List<Service> services = serviceRepository.findBySystemNameWithSettingNotNull(systemName);
        for (Service service : services) {
            checkDependenciesOfApp(service);
        }
    }

    //
    public boolean checkDependenciesOfApp(Service service) {
        String systemName = service.getSystemName();
        boolean update = false;
        Setting setting = service.getSetting();
        String notiTitle = "Found exception";
        // Check strong dependency
        if (setting.getEnableStrongDependencyAlert()) {
            if (setting.getStrongUpperDependencyCount() != null) {
                Integer strongUpperDependencyCount = generalRepository.getStrongUpperDependencyServiceCountByIdAndSystemName(service.getId(), systemName);
                if (strongUpperDependencyCount != null && strongUpperDependencyCount > setting.getStrongUpperDependencyCount()) {
                    if (service.addLabel(Service.LABEL_HEAVY_STRONG_UPPER_DEPENDENCY)) {
                        update = true;
                    }
                    // Push strong upper dependency exception notification
                    notificationService.pushNotificationToSystem(systemName, new DependencyWarningNotification(service.getAppName(),
                            service.getVersion(), strongUpperDependencyCount, setting.getStrongUpperDependencyCount(),
                            DependencyWarningNotification.THRESHOLD_STRONG_UPPER_DEPENDENCY));
                    logger.info("Found heavy strong upper dependency exception: " + service.getAppId());
                } else {
                    if (service.removeLabel(Service.LABEL_HEAVY_STRONG_UPPER_DEPENDENCY)) {
                        update = true;
                    }
                }
            }
            if (setting.getStrongLowerDependencyCount() != null) {
                Integer strongLowerDependencyCount = generalRepository.getStrongLowerDependencyServiceCountByIdAndSystemName(service.getId(), systemName);
                if (strongLowerDependencyCount != null && strongLowerDependencyCount > setting.getStrongLowerDependencyCount()) {
                    if (service.addLabel(Service.LABEL_HEAVY_STRONG_LOWER_DEPENDENCY)) {
                        update = true;
                    }
                    // Push strong lower dependency exception notification
                    notificationService.pushNotificationToSystem(systemName, new DependencyWarningNotification(service.getAppName(),
                            service.getVersion(), strongLowerDependencyCount, setting.getStrongLowerDependencyCount(),
                            DependencyWarningNotification.THRESHOLD_STRONG_LOWER_DEPENDENCY));
                    logger.info("Found heavy strong lower dependency exception: " + service.getAppId());
                } else {
                    if (service.removeLabel(Service.LABEL_HEAVY_STRONG_LOWER_DEPENDENCY)) {
                        update = true;
                    }
                }
            }
        } else if (service.removeLabel(Service.LABEL_HEAVY_STRONG_UPPER_DEPENDENCY) ||
                service.removeLabel(Service.LABEL_HEAVY_STRONG_LOWER_DEPENDENCY)){
            update = true;
        }
        // Check weak dependency
        if (setting.getEnableWeakDependencyAlert()) {
            if (setting.getWeakUpperDependencyCount() != null) {
                Integer weakUpperDependencyCount = generalRepository.getWeakUpperDependencyServiceCountByIdAndSystemName(service.getId(), systemName);
                if (weakUpperDependencyCount != null && weakUpperDependencyCount > setting.getWeakUpperDependencyCount()) {
                    if (service.addLabel(Service.LABEL_HEAVY_WEAK_UPPER_DEPENDENCY)) {
                        update = true;
                    }
                    // Push weak upper dependency exception notification
                    notificationService.pushNotificationToSystem(systemName, new DependencyWarningNotification(service.getAppName(),
                            service.getVersion(), weakUpperDependencyCount, setting.getWeakUpperDependencyCount(),
                            DependencyWarningNotification.THRESHOLD_WEAK_UPPER_DEPENDENCY));
                    logger.info("Found heavy weak upper dependency exception: " + service.getAppId());
                } else {
                    if (service.removeLabel(Service.LABEL_HEAVY_WEAK_UPPER_DEPENDENCY)) {
                        update = true;
                    }
                }
            }
            if (setting.getWeakLowerDependencyCount() != null) {
                Integer weakLowrDependencyCount = generalRepository.getWeakLowerDependencyServiceCountByIdAndSystemName(service.getId(), systemName);
                if (weakLowrDependencyCount != null && weakLowrDependencyCount > setting.getWeakLowerDependencyCount()) {
                    if (service.addLabel(Service.LABEL_HEAVY_WEAK_LOWER_DEPENDENCY)) {
                        update = true;
                    }
                    // Push weak lower dependency exception notification
                    notificationService.pushNotificationToSystem(systemName, new DependencyWarningNotification(service.getAppName(),
                            service.getVersion(), weakLowrDependencyCount, setting.getWeakLowerDependencyCount(),
                            DependencyWarningNotification.THRESHOLD_WEAK_LOWER_DEPENDENCY));
                    logger.info("Found heavy weak lower dependency exception: " + service.getAppId());
                } else {
                    if (service.removeLabel(Service.LABEL_HEAVY_WEAK_LOWER_DEPENDENCY)) {
                        update = true;
                    }
                }
            }
        } else if (service.removeLabel(Service.LABEL_HEAVY_WEAK_UPPER_DEPENDENCY) ||
                service.removeLabel(Service.LABEL_HEAVY_WEAK_LOWER_DEPENDENCY)){
            update = true;
        }
        if (update) {
            serviceRepository.save(service);
        }
        return update;
    }

}
