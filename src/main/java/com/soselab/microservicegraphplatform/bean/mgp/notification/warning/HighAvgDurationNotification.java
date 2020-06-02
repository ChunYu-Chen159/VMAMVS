package com.soselab.microservicegraphplatform.bean.mgp.notification.warning;

import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.WarningNotification;

import com.soselab.microservicegraphplatform.botmq.MSABotSender;

public class HighAvgDurationNotification extends WarningNotification {
    public static final String DATA_ACTUATOR = "Spring Actuator";
    public static final String DATA_ELASTICSEARCH = "Elasticsearch";

    private String appName;
    private String version;

    public HighAvgDurationNotification() {
    }

    public HighAvgDurationNotification(String appName, String version, Integer value, Integer threshold, String dataType) {
        super("High average duration", createContent(appName, version, value, threshold, dataType), createHtmlContent(appName, version, value, threshold, dataType));
        this.appName = appName;
        this.version = version;
    }

    private static String createContent(String appName, String version, Integer value, Integer threshold, String dataType) {
        String content = "Service \"" + appName + ":" + version +
                "\" exceeded the threshold of \"average duration\": current value (" + dataType + ") = " +
                value + ", threshold = " + threshold;
        MSABotSender msaBotSender = new MSABotSender();
        //msaBotSender.send(content,WebNotification.LEVEL_WARNING);

        return content;
    }

    private static String createHtmlContent(String appName, String version, Integer value, Integer threshold, String dataType) {
        return "Service <strong>" + appName + ":" + version +
                "</strong> exceeded the threshold of <strong>average duration</strong>: current value (" + dataType + ") = " +
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
