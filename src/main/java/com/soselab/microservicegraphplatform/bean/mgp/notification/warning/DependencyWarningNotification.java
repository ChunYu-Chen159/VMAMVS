package com.soselab.microservicegraphplatform.bean.mgp.notification.warning;

import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;
import com.soselab.microservicegraphplatform.bean.mgp.notification.WarningNotification;

import com.soselab.microservicegraphplatform.botmq.MSABotSender;

public class DependencyWarningNotification extends WarningNotification {
    public static final String THRESHOLD_STRONG_UPPER_DEPENDENCY = "strong upper dependency count";
    public static final String THRESHOLD_STRONG_LOWER_DEPENDENCY = "strong lower dependency count";
    public static final String THRESHOLD_WEAK_UPPER_DEPENDENCY = "weak upper dependency count";
    public static final String THRESHOLD_WEAK_LOWER_DEPENDENCY = "weak lower dependency count";

    private String appName;
    private String version;

    public DependencyWarningNotification() {
    }

    public DependencyWarningNotification(String appName, String version, Integer value, Integer threshold, String thresholdType) {
        super(createTitle(thresholdType), createContent(appName, version, value, threshold, thresholdType),
                createHtmlContent(appName, version, value, threshold, thresholdType));
        this.appName = appName;
        this.version = version;
    }

    private static String createTitle(String thresholdType) {
        if (thresholdType.equals(THRESHOLD_STRONG_UPPER_DEPENDENCY) || thresholdType.equals(THRESHOLD_WEAK_UPPER_DEPENDENCY)) {
            return "Heavy upper dependency";
        } else {
            return "Heavy lower dependency";
        }
    }

    private static String createContent(String appName, String version, Integer value, Integer threshold, String thresholdType) {
        String content = "Service \"" + appName + ":" + version +
                "\" exceeded the threshold of \"" + thresholdType + "\": current value = " +
                value + ", threshold = " + threshold;
        MSABotSender msaBotSender = new MSABotSender();
        //msaBotSender.send(content,WebNotification.LEVEL_WARNING);

        return content;
    }

    private static String createHtmlContent(String appName, String version, Integer value, Integer threshold, String thresholdType) {
        return "Service <strong>" + appName + ":" + version +
                "</strong> exceeded the threshold of <strong>" + thresholdType + "</strong>: current value = " +
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
