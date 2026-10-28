package com.stockpilot.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: no Spring, no WMS needed. */
class WmsOrderRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsWebStoreFieldsToWmsFields() throws Exception {
        String json = """
                {"order_id":"WEB-9","customer_code":"ACME","items":[{"sku":"GLV-L","qty":2,"price":8.99}]}
                """;
        WmsOrderRequest request = WmsOrderRequest.from(mapper.readValue(json, WebStoreOrder.class));

        assertThat(request.externalRef()).isEqualTo("WEB-9");
        assertThat(request.customerCode()).isEqualTo("ACME");
        assertThat(request.lines()).hasSize(1);
        assertThat(request.lines().get(0).partNumber()).isEqualTo("GLV-L");
        assertThat(request.lines().get(0).quantity()).isEqualByComparingTo("2");
    }

    @Test
    void ordersWithoutCustomerGoToWebCustomer() throws Exception {
        String json = """
                {"order_id":"WEB-10","items":[{"sku":"ZIP-08","qty":100,"price":0.15}]}
                """;
        WmsOrderRequest request = WmsOrderRequest.from(mapper.readValue(json, WebStoreOrder.class));

        assertThat(request.customerCode()).isEqualTo("WEB");
    }
}
