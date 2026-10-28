package com.stockpilot.connector;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Talks to the WMS REST API. Logs in once, keeps the token, and logs in again
 * if the WMS says the token is no longer valid (for example after a server restart).
 */
@Component
public class WmsApiClient {

    private static final Logger log = LoggerFactory.getLogger(WmsApiClient.class);

    private final RestClient http;
    private final ConnectorProperties props;
    private String token;

    public WmsApiClient(RestClient.Builder builder, ConnectorProperties props) {
        this.http = builder.baseUrl(props.wmsBaseUrl()).build();
        this.props = props;
    }

    /** Creates the order and returns the WMS response (includes soNumber). */
    public JsonNode createSalesOrder(WmsOrderRequest request) {
        try {
            return postOrder(request);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.info("Token rejected, logging in again");
                token = null;
                return postOrder(request);
            }
            throw e;
        }
    }

    private JsonNode postOrder(WmsOrderRequest request) {
        return http.post()
                .uri("/api/sales-orders")
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
    }

    private synchronized String token() {
        if (token == null) {
            JsonNode response = http.post()
                    .uri("/api/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "appName", props.appName(),
                            "appId", props.appId(),
                            "appDescription", "Imports web store orders",
                            "username", props.username(),
                            "password", props.password()))
                    .retrieve()
                    .body(JsonNode.class);
            token = response.get("token").asText();
            log.info("Logged in to WMS at {} as {}", props.wmsBaseUrl(), props.username());
        }
        return token;
    }
}
