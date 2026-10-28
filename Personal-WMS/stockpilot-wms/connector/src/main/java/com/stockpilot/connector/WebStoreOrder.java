package com.stockpilot.connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * An order as the web store sends it. Note the different field names from the WMS API
 * (order_id vs externalRef, sku vs partNumber): mapping between the two is the connector's job.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebStoreOrder(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("customer_code") String customerCode,
        @JsonProperty("items") List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("sku") String sku,
            @JsonProperty("qty") BigDecimal qty,
            @JsonProperty("price") BigDecimal price) {
    }
}
