package com.soselab.microservicegraphplatform.bean.mgp.notification.info;

import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;

import java.time.LocalDateTime;

import com.soselab.microservicegraphplatform.botmq.MSABotSender;

public class ServiceUpNotification extends WebNotification {
    private String appName;
    private String version;

    public ServiceUpNotification() {
    }

    public ServiceUpNotification(String appName, String version) {
        super(WebNotification.LEVEL_INFO, "Service up", createContent(appName, version),
                createHtmlContent(appName, version), LocalDateTime.now().plusHours(8));
        this.appName = appName;
        this.version = version;
    }

    private static String createContent(String appName, String version) {

        String content = "Service \"" + appName + " : " + version + "\" is up.";
        MSABotSender msaBotSender = new MSABotSender();

        //msaBotSender.send(content, WebNotification.LEVEL_WARNING);


        return content;
    }

    private static String createHtmlContent(String appName, String version) {
        return "Service <strong>" + appName + " : " + version + "</strong> is up.";
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
