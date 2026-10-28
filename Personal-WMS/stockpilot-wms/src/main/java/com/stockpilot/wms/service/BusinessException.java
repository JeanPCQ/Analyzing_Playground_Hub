package com.stockpilot.wms.service;

/** A rule was broken (e.g. "can't receive more than ordered"). The message is shown to the user. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
