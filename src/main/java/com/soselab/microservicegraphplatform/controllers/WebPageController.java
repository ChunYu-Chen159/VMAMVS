package com.soselab.microservicegraphplatform.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.MonitorError;
import com.soselab.microservicegraphplatform.bean.mgp.monitor.SpcData;
import com.soselab.microservicegraphplatform.services.*;
import com.soselab.microservicegraphplatform.bean.mgp.AppSetting;
import com.soselab.microservicegraphplatform.bean.neo4j.Service;
import com.soselab.microservicegraphplatform.bean.neo4j.Setting;
import com.soselab.microservicegraphplatform.repositories.neo4j.ServiceRepository;
import com.soselab.microservicegraphplatform.repositories.neo4j.SettingRepository;
import com.soselab.microservicegraphplatform.bean.mgp.AppMetrics;
import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;
import com.soselab.microservicegraphplatform.repositories.neo4j.GeneralRepository;
import com.sun.tools.javac.resources.version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/web-page")
public class WebPageController {

    private static final Logger logger = LoggerFactory.getLogger(WebPageController.class);

    @Autowired
    private GeneralRepository generalRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private SettingRepository settingRepository;
    @Autowired
    private GraphService graphService;
    @Autowired
    private MonitorService monitorService;
    @Autowired
    private WebNotificationService notificationService;
    @Autowired
    private SpringRestTool springRestTool;
    @Autowired
    private LogAnalyzer logAnalyzer;
    @Autowired
    private RestInfoAnalyzer restInfoAnalyzer;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private SleuthService sleuthService;
    @Autowired
    private ContractService contractService;
    @Autowired
    private MonitorErrorSimulator monitorErrorSimulator;

    @GetMapping("/system-names")
    public String getSystems() {
        List<String> sysNames = generalRepository.getAllSystemName();
        String result = null;
        try {
            result = mapper.writeValueAsString(sysNames);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    @GetMapping("/services/{systemName}")
    public String getServices(@PathVariable("systemName") String systemName) {
        List<String> serviceNames = generalRepository.getSystemAllServiceName(systemName);
        String result = null;
        try {
            result = mapper.writeValueAsString(serviceNames);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    @GetMapping("/sleuth/getAllServiceAndPathWithHTTP_REQUEST")
    public String getAllServiceAndPathWithHTTP_REQUEST() {

        return sleuthService.calculateNumofRequest().toString();
    }

    @GetMapping("/sleuth/{appName}")
    public String getSleuthTrace(@PathVariable("appName") String appName) {
        return sleuthService.getTraceInfo(appName);
    }

    // 查詢從endTime往回推lookback時間的資料
    @GetMapping("/sleuth/searchZipkin/{appName}/{version}/{statusCode}/{lookback}/{endTime}/{limit}")
    public String searchZipkin(@PathVariable("appName") String appName,
                               @PathVariable("version") String version,
                               @PathVariable("statusCode") int statusCode,
                               @PathVariable("lookback") long lookback,
                               @PathVariable("endTime") long endTime,
                               @PathVariable("limit") int limit){
        return sleuthService.searchZipkin(appName, version, statusCode, lookback, endTime, limit);
    }

/*    @GetMapping("/contract/getAllServiceContractTestingCondition/{systemName}")
    public String getAllServiceContractTestingCondition(@PathVariable("systemName") String systemName) {
        return contractService.setAllServiceContractTestingCondition(systemName).toString();
    }*/


    @GetMapping("/graph/providers/{id}")
    public String getProviders(@PathVariable("id") long id) {
        return generalRepository.getProviders(id);
    }

    @GetMapping("/graph/consumers/{id}")
    public String getComsumers(@PathVariable("id") Long id) {
        return generalRepository.getConsumers(id);
    }

    @GetMapping("/graph/strong-upper-dependency-chain/{id}")
    public String getStrongDependencyChain(@PathVariable("id") Long id) {
        return generalRepository.getStrongUpperDependencyChainById(id);
    }

    @GetMapping("/graph/weak-upper-dependency-chain/{id}")
    public String getWeakDependencyChain(@PathVariable("id") Long id) {
        return generalRepository.getWeakUpperDependencyChainById(id);
    }

    @GetMapping("/graph/strong-lower-dependency-chain/{id}")
    public String getStrongSubordinateChain(@PathVariable("id") Long id) {
        return generalRepository.getStrongLowerDependencyChainById(id);
    }

    @GetMapping("/graph/weak-lower-dependency-chain/{id}")
    public String getWeakSubordinateChain(@PathVariable("id") Long id) {
        return generalRepository.getWeakLowerDependencyChainById(id);
    }

    @GetMapping("/app/swagger/{appId}")
    public String getSwagger(@PathVariable("appId") String appId) {
        String[] appInfo = appId.split(":");
        return springRestTool.getSwaggerFromRemoteApp(appInfo[0], appInfo[1], appInfo[2]);
    }

    @GetMapping("/app/metrics/log/{appId}")
    public AppMetrics getLogMetrics(@PathVariable("appId") String appId) {
        String[] appInfo = appId.split(":");
        return logAnalyzer.getMetrics(appInfo[0], appInfo[1], appInfo[2]);
    }

    @GetMapping("/app/metrics/rest/{appId}")
    public AppMetrics getRestMetrics(@PathVariable("appId") String appId) {
        String[] appInfo = appId.split(":");
        return restInfoAnalyzer.getMetrics(appInfo[0], appInfo[1], appInfo[2]);
    }

    @GetMapping("/app/setting/{appId}")
    public AppSetting getSetting(@PathVariable("appId") String appId) {
        return new AppSetting(settingRepository.findByConfigServiceAppId(appId));
    }

    @PostMapping("/app/setting/{appId}")
    public void postSetting(@PathVariable("appId") String appId, @RequestBody Setting setting) {
        if (setting != null) {
            Setting oldSetting = settingRepository.findByConfigServiceAppId(appId);
            if (oldSetting != null) {
                settingRepository.delete(oldSetting);
                setting.setConfigService(oldSetting.getConfigService());
                settingRepository.save(setting);
            } else {
                Service service = serviceRepository.findByAppId(appId);
                if (service != null) {
                    setting.setConfigService(service);
                    settingRepository.save(setting);
                }
            }
            logger.info(appId + " setting updated");
            graphService.appSettingUpdatedEvent(appId);
        }
    }

    @GetMapping("/graph/getGraphJson/{systemName}")
    public String getGraphJson(@PathVariable("systemName") String systemName)  {
        return graphService.getGraphJson(systemName);
    }

    @MessageMapping("/graph/{systemName}")
    @SendTo("/topic/graph/{systemName}")
    public String getGraph(@DestinationVariable String systemName) throws Exception {
        return graphService.getGraphJson(systemName);
    }

    public void sendGraph(String systemName, String data) {
        messagingTemplate.convertAndSend("/topic/graph/" + systemName, data);
    }

    @MessageMapping("/graph/spc/failureStatusRate/{systemName}")
    @SendTo("/topic/graph/spc/failureStatusRate/{systemName}")
    public SpcData getAppsFailureStatusRateSpc(@DestinationVariable("systemName") String systemName) {
        return monitorService.getFailureStatusRateSPC(systemName);
    }

    public void sendAppsFailureStatusRateSPC(String systemName, SpcData data) {
        messagingTemplate.convertAndSend("/topic/graph/spc/failureStatusRate/" + systemName, data);
    }

    @MessageMapping("/graph/spc/averageDuration/{systemName}")
    @SendTo("/topic/graph/spc/averageDuration/{systemName}")
    public SpcData getAppsAverageDurationSpc(@DestinationVariable("systemName") String systemName) {
        return monitorService.getAverageDurationSPC(systemName);
    }

    public void sendAppsAverageDurationSPC(String systemName, SpcData data) {
        messagingTemplate.convertAndSend("/topic/graph/spc/averageDuration/" + systemName, data);
    }

    @GetMapping("/app/spc/duration/{appId}")
    public SpcData getAppDurationSpc(@PathVariable("appId") String appId) {
        return monitorService.getAppDurationSPC(appId);
    }

    @GetMapping("/app/spc/ver-usage/{appId}")
    public SpcData getVersionUsageSpc(@PathVariable("appId") String appId) {
        return monitorService.getVersionUsageSPC(appId);
    }

    @GetMapping("/notification/{systemName}")
    public List<WebNotification> getNotifications(@PathVariable("systemName") String systemName) {
        return notificationService.getNotificationsOfSystem(systemName);
    }

    public void sendNotification(String systemName, WebNotification notification) {
        messagingTemplate.convertAndSend("/topic/notification/" + systemName, notification);
    }

    @GetMapping("/monitor/getErrors/{systemName}")
    public List<MonitorError> getErrorsOfSystem(@PathVariable("systemName") String systemName) {
        return monitorService.getErrorsOfSystem(systemName);
    }

    @GetMapping("/monitor/runMonitorErrors/{systemName}")
    public void runMonitorErrorsOfSystem(@PathVariable("systemName") String systemName) {
        monitorService.checkErrorFromSleuth(systemName);
    }

    @GetMapping("/monitor/simulateMonitorErrors/{systemName}")
    public List<MonitorError> simulateMonitorError(@PathVariable("systemName") String systemName) {
        return monitorErrorSimulator.simulateErrors(systemName);
    }
//
////    @RequestMapping(value = "/getImage/{systemName}",produces = MediaType.IMAGE_PNG_VALUE)
//    @RequestMapping(value = "/getImage/{systemName}")
//    @ResponseBody
//    public String getImage(@PathVariable("systemName") String systemName) throws IOException {
//        WebClient webclient = new WebClient();
//        HtmlPage htmlpage = webclient.getPage("140.121.197.128:4147");
//
//        DomElement download = htmlpage.getElementById("download-graph");
//
//        HtmlPage result_page = download.click();
//
//        String downloadURL = result_page.getElementById("download-graph").getAttribute("href");
//
//        return downloadURL;
////        return ImageIO.read(new FileInputStream(new File("D:/test.jpg")));
//
//    }

}
