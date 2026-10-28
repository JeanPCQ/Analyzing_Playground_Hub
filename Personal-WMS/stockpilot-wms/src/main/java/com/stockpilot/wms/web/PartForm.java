package com.stockpilot.wms.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** What the part screen sends. Validated before it reaches the service. */
public class PartForm {

    @NotBlank(message = "Part number is required")
    @Size(max = 40, message = "Part number can be at most 40 characters")
    private String partNumber;

    @NotBlank(message = "Description is required")
    @Size(max = 255)
    private String description;

    @NotNull(message = "Standard cost is required")
    @PositiveOrZero(message = "Standard cost can't be negative")
    private BigDecimal standardCost = BigDecimal.ZERO;

    @NotNull(message = "Reorder point is required")
    @PositiveOrZero(message = "Reorder point can't be negative")
    private BigDecimal reorderPoint = BigDecimal.ZERO;

    private boolean active = true;

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getStandardCost() { return standardCost; }
    public void setStandardCost(BigDecimal standardCost) { this.standardCost = standardCost; }
    public BigDecimal getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(BigDecimal reorderPoint) { this.reorderPoint = reorderPoint; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
