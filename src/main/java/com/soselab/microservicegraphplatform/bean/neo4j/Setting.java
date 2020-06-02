package com.soselab.microservicegraphplatform.bean.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Setting {

    @GraphId
    private Long id;

    private Boolean enableRestFailureAlert;
    private Boolean enableLogFailureAlert;
    private Float failureStatusRate;
    private Long failureErrorCount;

    private Boolean enableSPCHighDurationRateAlert;
    private Float thresholdSPCHighDurationRate;

    private Boolean enableRestAverageDurationAlert;
    private Boolean enableLogAverageDurationAlert;
    private Integer thresholdAverageDuration;

    private Boolean enableStrongDependencyAlert;
    private Integer strongUpperDependencyCount;
    private Integer strongLowerDependencyCount;
    private Boolean enableWeakDependencyAlert;
    private Integer weakUpperDependencyCount;
    private Integer weakLowerDependencyCount;

    private Float riskValueAlert;

    public Setting() {
    }

    public Setting(Boolean enableRestFailureAlert, Boolean enableLogFailureAlert, Float failureStatusRate, Long failureErrorCount, Boolean enableSPCHighDurationRateAlert, Float thresholdSPCHighDurationRate, Boolean enableRestAverageDurationAlert, Boolean enableLogAverageDurationAlert, Integer thresholdAverageDuration, Boolean enableStrongDependencyAlert, Integer strongUpperDependencyCount, Integer strongLowerDependencyCount, Boolean enableWeakDependencyAlert, Integer weakUpperDependencyCount, Integer weakLowerDependencyCount, Float riskValueAlert) {
        this.enableRestFailureAlert = enableRestFailureAlert;
        this.enableLogFailureAlert = enableLogFailureAlert;
        this.failureStatusRate = failureStatusRate;
        this.failureErrorCount = failureErrorCount;
        this.enableSPCHighDurationRateAlert = enableSPCHighDurationRateAlert;
        this.thresholdSPCHighDurationRate = thresholdSPCHighDurationRate;
        this.enableRestAverageDurationAlert = enableRestAverageDurationAlert;
        this.enableLogAverageDurationAlert = enableLogAverageDurationAlert;
        this.thresholdAverageDuration = thresholdAverageDuration;
        this.enableStrongDependencyAlert = enableStrongDependencyAlert;
        this.strongUpperDependencyCount = strongUpperDependencyCount;
        this.strongLowerDependencyCount = strongLowerDependencyCount;
        this.enableWeakDependencyAlert = enableWeakDependencyAlert;
        this.weakUpperDependencyCount = weakUpperDependencyCount;
        this.weakLowerDependencyCount = weakLowerDependencyCount;
        this.riskValueAlert = riskValueAlert;
    }

    public Setting(Setting setting) {
        if (setting != null) {
            this.enableRestFailureAlert = setting.enableRestFailureAlert;
            this.enableLogFailureAlert = setting.enableLogFailureAlert;
            this.failureStatusRate = setting.failureStatusRate;
            this.failureErrorCount = setting.failureErrorCount;
            this.enableSPCHighDurationRateAlert = setting.enableSPCHighDurationRateAlert;
            this.thresholdSPCHighDurationRate = setting.thresholdSPCHighDurationRate;
            this.enableRestAverageDurationAlert = setting.enableRestAverageDurationAlert;
            this.enableLogAverageDurationAlert = setting.enableLogAverageDurationAlert;
            this.thresholdAverageDuration = setting.thresholdAverageDuration;
            this.enableStrongDependencyAlert = setting.enableStrongDependencyAlert;
            this.strongUpperDependencyCount = setting.strongUpperDependencyCount;
            this.strongLowerDependencyCount = setting.strongLowerDependencyCount;
            this.enableWeakDependencyAlert = setting.enableWeakDependencyAlert;
            this.weakUpperDependencyCount = setting.weakUpperDependencyCount;
            this.weakLowerDependencyCount = setting.weakLowerDependencyCount;
            this.riskValueAlert = setting.riskValueAlert;
        }
    }

    public Long getId() {
        return id;
    }

    public Boolean getEnableRestFailureAlert() {
        return enableRestFailureAlert;
    }

    public void setEnableRestFailureAlert(Boolean enableRestFailureAlert) {
        this.enableRestFailureAlert = enableRestFailureAlert;
    }

    public Boolean getEnableLogFailureAlert() {
        return enableLogFailureAlert;
    }

    public void setEnableLogFailureAlert(Boolean enableLogFailureAlert) {
        this.enableLogFailureAlert = enableLogFailureAlert;
    }

    public Float getFailureStatusRate() {
        return failureStatusRate;
    }

    public void setFailureStatusRate(Float failureStatusRate) {
        this.failureStatusRate = failureStatusRate;
    }

    public Long getFailureErrorCount() {
        return failureErrorCount;
    }

    public void setFailureErrorCount(Long failureErrorCount) {
        this.failureErrorCount = failureErrorCount;
    }

    public Boolean getEnableSPCHighDurationRateAlert() {
        return enableSPCHighDurationRateAlert;
    }

    public void setEnableSPCHighDurationRateAlert(Boolean enableSPCHighDurationRateAlert) {
        this.enableSPCHighDurationRateAlert = enableSPCHighDurationRateAlert;
    }

    public Float getThresholdSPCHighDurationRate() {
        return thresholdSPCHighDurationRate;
    }

    public void setThresholdSPCHighDurationRate(Float thresholdSPCHighDurationRate) {
        this.thresholdSPCHighDurationRate = thresholdSPCHighDurationRate;
    }

    public Boolean getEnableRestAverageDurationAlert() {
        return enableRestAverageDurationAlert;
    }

    public void setEnableRestAverageDurationAlert(Boolean enableRestAverageDurationAlert) {
        this.enableRestAverageDurationAlert = enableRestAverageDurationAlert;
    }

    public Boolean getEnableLogAverageDurationAlert() {
        return enableLogAverageDurationAlert;
    }

    public void setEnableLogAverageDurationAlert(Boolean enableLogAverageDurationAlert) {
        this.enableLogAverageDurationAlert = enableLogAverageDurationAlert;
    }

    public Integer getThresholdAverageDuration() {
        return thresholdAverageDuration;
    }

    public void setThresholdAverageDuration(Integer thresholdAverageDuration) {
        this.thresholdAverageDuration = thresholdAverageDuration;
    }

    public Boolean getEnableStrongDependencyAlert() {
        return enableStrongDependencyAlert;
    }

    public void setEnableStrongDependencyAlert(Boolean enableStrongDependencyAlert) {
        this.enableStrongDependencyAlert = enableStrongDependencyAlert;
    }

    public Integer getStrongUpperDependencyCount() {
        return strongUpperDependencyCount;
    }

    public void setStrongUpperDependencyCount(Integer strongUpperDependencyCount) {
        this.strongUpperDependencyCount = strongUpperDependencyCount;
    }

    public Integer getStrongLowerDependencyCount() {
        return strongLowerDependencyCount;
    }

    public void setStrongLowerDependencyCount(Integer strongLowerDependencyCount) {
        this.strongLowerDependencyCount = strongLowerDependencyCount;
    }

    public Boolean getEnableWeakDependencyAlert() {
        return enableWeakDependencyAlert;
    }

    public void setEnableWeakDependencyAlert(Boolean enableWeakDependencyAlert) {
        this.enableWeakDependencyAlert = enableWeakDependencyAlert;
    }

    public Integer getWeakUpperDependencyCount() {
        return weakUpperDependencyCount;
    }

    public void setWeakUpperDependencyCount(Integer weakUpperDependencyCount) {
        this.weakUpperDependencyCount = weakUpperDependencyCount;
    }

    public Integer getWeakLowerDependencyCount() {
        return weakLowerDependencyCount;
    }

    public void setWeakLowerDependencyCount(Integer weakLowerDependencyCount) {
        this.weakLowerDependencyCount = weakLowerDependencyCount;
    }

    public Float getRiskValueAlert() {
        return riskValueAlert;
    }

    public void setRiskValueAlert(Float riskValueAlert) {
        this.riskValueAlert = riskValueAlert;
    }

    @Relationship(type = "MGP_CONFIG", direction = Relationship.INCOMING)
    private Service configService;

    public Service getConfigService() {
        return configService;
    }

    public void setConfigService(Service configService) {
        this.configService = configService;
    }

}
