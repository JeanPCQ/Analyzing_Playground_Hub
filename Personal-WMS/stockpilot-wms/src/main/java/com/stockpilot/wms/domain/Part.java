package com.stockpilot.wms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** An item the warehouse stocks. Quantities are always in EA. */
@Entity
@Table(name = "part")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_number", nullable = false, unique = true, length = 40)
    private String partNumber;

    @Column(nullable = false)
    private String description;

    @Column(name = "standard_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal standardCost = BigDecimal.ZERO;

    @Column(name = "reorder_point", nullable = false, precision = 19, scale = 4)
    private BigDecimal reorderPoint = BigDecimal.ZERO;

    /** Quantity promised to open sales orders. Maintained by SalesOrderService. */
    @Column(name = "qty_allocated", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyAllocated = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getStandardCost() { return standardCost; }
    public void setStandardCost(BigDecimal standardCost) { this.standardCost = standardCost; }
    public BigDecimal getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(BigDecimal reorderPoint) { this.reorderPoint = reorderPoint; }
    public BigDecimal getQtyAllocated() { return qtyAllocated; }
    public void setQtyAllocated(BigDecimal qtyAllocated) { this.qtyAllocated = qtyAllocated; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
