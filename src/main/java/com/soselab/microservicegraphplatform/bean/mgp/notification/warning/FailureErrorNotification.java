package com.soselab.microservicegraphplatform.bean.mgp.notification.warning;

import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.WarningNotification;

import com.soselab.microservicegraphplatform.botmq.MSABotSender;

public class FailureErrorNotification extends WarningNotification {
    public static final String TYPE_ACTUATOR = "Spring Actuator";
    public static final String TYPE_ELASTICSEARCH = "Elasticsearch";

    private String appName;
    private String version;

    public FailureErrorNotification() {
    }

    public FailureErrorNotification(String appName, String version, Long value, Long threshold, String dataType) {
        super("Service error", createContent(appName, version, value, threshold, dataType),
                createHtmlContent(appName, version, value, threshold, dataType));
        this.appName = appName;
        this.version = version;
    }

    private static String createContent(String appName, String version, Long value, Long threshold, String dataType) {
        String content = "Service \"" + appName + ":" + version +
                "\" exceeded the threshold of \"error count\": current value (" + dataType + ") = " +
                value + ", threshold = " + threshold;
        MSABotSender msaBotSender = new MSABotSender();

        msaBotSender.send(content, WebNotification.LEVEL_WARNING);


        return content;
    }

    private static String createHtmlContent(String appName, String version, Long value, Long threshold, String dataType) {
        return "Service <strong>" + appName + ":" + version +
                "</strong> exceeded the threshold of <strong>error count</strong>: current value (" + dataType + ") = " +
                value + ", threshold = " + threshold;
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
