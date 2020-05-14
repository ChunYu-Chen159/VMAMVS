package com.soselab.microservicegraphplatform.services;

import com.soselab.microservicegraphplatform.bean.mgp.WebNotification;
import com.soselab.microservicegraphplatform.controllers.WebPageController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class WebNotificationService {

    @Autowired
    private WebPageController webPageController;
    private Map<String, List<WebNotification>> allNotificationList = new HashMap<>();

    public void pushNotificationToSystem(String systemName, WebNotification notification) {
        webPageController.sendNotification(systemName, notification);
        allNotificationList.merge(systemName, new ArrayList<>(Arrays.asList(notification)),
                (oldList, newList) -> pushNotification(oldList, notification));
    }

    private List<WebNotification> pushNotification(List<WebNotification> notifications, WebNotification notification) {
        if (notifications.size() == 100) {
            notifications.remove(99);
        } else {
            notifications.add(0, notification);
        }
        return notifications;
    }

    public List<WebNotification> getNotificationsOfSystem(String systemName) {
        return allNotificationList.getOrDefault(systemName, new ArrayList<>());
    }
}
