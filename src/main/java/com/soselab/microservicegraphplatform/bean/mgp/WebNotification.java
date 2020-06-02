package com.soselab.microservicegraphplatform.bean.mgp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.soselab.microservicegraphplatform.botmq.MSABotSender;

import java.time.LocalDateTime;
import java.util.Date;

public class WebNotification {

    @JsonIgnore
    public static final String LEVEL_INFO = "info";
    @JsonIgnore
    public static final String LEVEL_WARNING = "warning";
    @JsonIgnore
    public static final String LEVEL_ERROR = "error";

    private String level;
    private String title;
    private String content;
    private String htmlContent;
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime dateTime;

    public WebNotification() {
    }

    public WebNotification(String level, String title, String content, String htmlContent, LocalDateTime dateTime) {
        this.level = level;
        this.title = title;
        this.content = content;
        this.htmlContent = htmlContent;
        this.dateTime = dateTime;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

}
