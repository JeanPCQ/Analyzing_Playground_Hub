package com.stockpilot.wms.service;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(message);
    }
}
