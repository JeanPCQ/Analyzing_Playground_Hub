package com.stockpilot.connector;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connector")
public record ConnectorProperties(
        String wmsBaseUrl,
        String username,
        String password,
        String appName,
        String appId,
        String inboxDir,
        String processedDir,
        String failedDir,
        long pollIntervalMs) {
}
