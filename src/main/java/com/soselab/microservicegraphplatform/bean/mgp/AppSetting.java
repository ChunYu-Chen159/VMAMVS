package com.soselab.microservicegraphplatform.bean.mgp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.soselab.microservicegraphplatform.bean.neo4j.Setting;

@JsonIgnoreProperties({"id", "configService"})
public class AppSetting extends Setting {
    public AppSetting() {
    }

    public AppSetting(Boolean enableRestFailureAlert, Boolean enableLogFailureAlert, Float failureStatusRate, Long failureErrorCount, Boolean enableSPCHighDurationRateAlert, Float thresholdSPCHighDurationRate, Boolean enableRestAverageDurationAlert, Boolean enableLogAverageDurationAlert, Integer thresholdAverageDuration, Boolean enableStrongDependencyAlert, Integer strongUpperDependencyCount, Integer strongLowerDependencyCount, Boolean enableWeakDependencyAlert, Integer weakUpperDependencyCount, Integer weakLowerDependencyCount, Double riskValueAlert, Boolean enableRiskValueAlert) {
        super(enableRestFailureAlert, enableLogFailureAlert, failureStatusRate, failureErrorCount, enableSPCHighDurationRateAlert, thresholdSPCHighDurationRate, enableRestAverageDurationAlert, enableLogAverageDurationAlert, thresholdAverageDuration, enableStrongDependencyAlert, strongUpperDependencyCount, strongLowerDependencyCount, enableWeakDependencyAlert, weakUpperDependencyCount, weakLowerDependencyCount, riskValueAlert, enableRiskValueAlert);
    }

    public AppSetting(Setting setting) {
        super(setting);
    }
}
