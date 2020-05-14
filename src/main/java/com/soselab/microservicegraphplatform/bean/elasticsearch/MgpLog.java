package com.soselab.microservicegraphplatform.bean.elasticsearch;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.sql.Date;
import java.sql.Timestamp;

@Document(indexName = "*", type = "doc", createIndex = false)
public class MgpLog {
    @Id
    private String id;

    private String host;
    private String level;
    @JsonProperty("@timestamp")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="GMT")
    private Timestamp timestamp;
    @JsonProperty("logger_name")
    private String loggerName;
    @JsonProperty("level_value")
    private int levelValue;
    private String systemName;
    @JsonProperty("thread_name")
    private String threadName;
    private int port;
    private String appName;
    private String version;
    //@JsonDeserialize
    private String message;

    public MgpLog() {
    }

    public MgpLog(String host, String level, Timestamp timestamp, String loggerName, int levelValue, String systemName, String threadName, int port, String appName, String version, String message) {
        this.host = host;
        this.level = level;
        this.timestamp = timestamp;
        this.loggerName = loggerName;
        this.levelValue = levelValue;
        this.systemName = systemName;
        this.threadName = threadName;
        this.port = port;
        this.appName = appName;
        this.version = version;
        this.message = message;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public int getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(int levelValue) {
        this.levelValue = levelValue;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
