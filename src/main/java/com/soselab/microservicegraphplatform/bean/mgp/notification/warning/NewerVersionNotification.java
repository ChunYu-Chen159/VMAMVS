package com.soselab.microservicegraphplatform.bean.mgp.notification.warning;

import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.WarningNotification;

import com.soselab.microservicegraphplatform.botmq.MSABotSender;

public class NewerVersionNotification extends WarningNotification {
    public static final String LEVEL_MAJOR = "major";
    public static final String LEVEL_MINOR  = "minor";
    public static final String LEVEL_PATCH   = "patch";

    private String appName;
    private String version;

    public NewerVersionNotification() {
    }

    public NewerVersionNotification(String appName, String version, String newerVersion, String level) {
        super("Found newer version", createContent(appName, version, newerVersion, level),
                createHtmlContent(appName, version, newerVersion, level));
        this.appName = appName;
        this.version = version;
    }

    private static String createContent(String appName, String version, String newerVersion, String level) {
        String content = "Found newer " + level + " version of service: \"" + appName + ":" + version + " → " + newerVersion + "\"";
        MSABotSender msaBotSender = new MSABotSender();

        msaBotSender.send(content, WebNotification.LEVEL_WARNING);


        return content;
    }

    private static String createHtmlContent(String appName, String version, String newerVersion, String level) {
        return "Found newer " + level + " version of service: <strong>" + appName + ":" + version + " → " + newerVersion + "</strong>";
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
