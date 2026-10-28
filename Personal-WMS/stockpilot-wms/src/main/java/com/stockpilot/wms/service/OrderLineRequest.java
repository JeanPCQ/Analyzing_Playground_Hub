package com.stockpilot.wms.service;

import java.math.BigDecimal;

/** One line of an order coming from outside (API, import). */
public record OrderLineRequest(String partNumber, BigDecimal quantity, BigDecimal unitPrice) {
}
