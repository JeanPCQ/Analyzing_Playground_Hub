package com.stockpilot.connector;

import java.math.BigDecimal;
import java.util.List;

/** The body the WMS expects at POST /api/sales-orders. */
public record WmsOrderRequest(String customerCode, String externalRef, List<Line> lines) {

    public record Line(String partNumber, BigDecimal quantity, BigDecimal unitPrice) {
    }

    /** Web store format -> WMS format. Web orders without a customer code go to the WEB customer. */
    public static WmsOrderRequest from(WebStoreOrder order) {
        String customer = (order.customerCode() == null || order.customerCode().isBlank())
                ? "WEB" : order.customerCode();
        List<Line> lines = order.items().stream()
                .map(i -> new Line(i.sku(), i.qty(), i.price()))
                .toList();
        return new WmsOrderRequest(customer, order.orderId(), lines);
    }
}
