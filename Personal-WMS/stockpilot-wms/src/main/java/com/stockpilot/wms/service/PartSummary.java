package com.stockpilot.wms.service;

import com.stockpilot.wms.domain.Part;

import java.math.BigDecimal;

/** A part plus its stock numbers, ready for display. */
public class PartSummary {

    private final Part part;
    private final BigDecimal onHand;

    public PartSummary(Part part, BigDecimal onHand) {
        this.part = part;
        this.onHand = onHand;
    }

    public Part getPart() { return part; }

    public BigDecimal getOnHand() { return onHand; }

    public BigDecimal getAllocated() { return part.getQtyAllocated(); }

    /** On hand minus what is promised to open sales orders. */
    public BigDecimal getAvailable() { return onHand.subtract(part.getQtyAllocated()); }

    public boolean isLowStock() { return getAvailable().compareTo(part.getReorderPoint()) < 0; }
}
