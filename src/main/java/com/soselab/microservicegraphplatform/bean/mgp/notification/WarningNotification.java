package com.soselab.microservicegraphplatform.bean.mgp.notification;

import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;

import java.time.LocalDateTime;

public class WarningNotification extends WebNotification {

    public WarningNotification() {
    }

    public WarningNotification(String title, String content, String htmlContent) {
        super(WebNotification.LEVEL_WARNING, title, content, htmlContent, LocalDateTime.now().plusHours(8));
    }

}
