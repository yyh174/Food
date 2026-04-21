package com.xl.can.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "review.monitor")
public class ReviewMonitorProperties {

    private boolean enabled = true;

    private String cron = "0 */5 * * * ?";

    private int starThreshold = 3;

    private int scanWindowMinutes = 30;

    private boolean autoCreateTicket = true;
}
