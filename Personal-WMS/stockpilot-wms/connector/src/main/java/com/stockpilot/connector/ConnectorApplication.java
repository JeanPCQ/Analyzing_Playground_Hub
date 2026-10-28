package com.stockpilot.connector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Simulates a third-party integration: a web store drops order files into inbox/,
 * and this service logs in to the WMS API and creates sales orders from them.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class ConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectorApplication.class, args);
    }
}
