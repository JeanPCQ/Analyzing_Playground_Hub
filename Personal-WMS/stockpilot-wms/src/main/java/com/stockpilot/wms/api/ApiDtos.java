package com.stockpilot.wms.api;

import com.stockpilot.wms.domain.InventoryLevel;
import com.stockpilot.wms.domain.SalesOrder;
import com.stockpilot.wms.service.PartSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request and response shapes for the REST API. Kept separate from entities on purpose. */
public final class ApiDtos {

    private ApiDtos() { }

    public record LoginRequest(
            @NotBlank String appName,
            @NotBlank String appId,
            String appDescription,
            @NotBlank String username,
            @NotBlank String password) { }

    public record LoginResponse(String token, String username, long expiresAfterIdleMinutes) { }

    public record PartDto(String partNumber, String description, BigDecimal standardCost, BigDecimal reorderPoint,
                          BigDecimal onHand, BigDecimal allocated, BigDecimal available, boolean active) {
        public static PartDto from(PartSummary s) {
            return new PartDto(s.getPart().getPartNumber(), s.getPart().getDescription(),
                    s.getPart().getStandardCost(), s.getPart().getReorderPoint(),
                    s.getOnHand(), s.getAllocated(), s.getAvailable(), s.getPart().isActive());
        }
    }

    public record CreatePartRequest(
            @NotBlank String partNumber,
            @NotBlank String description,
            @NotNull @PositiveOrZero BigDecimal standardCost,
            @NotNull @PositiveOrZero BigDecimal reorderPoint) { }

    public record InventoryLevelDto(String partNumber, String locationCode, BigDecimal qtyOnHand) {
        public static InventoryLevelDto from(InventoryLevel level) {
            return new InventoryLevelDto(level.getPart().getPartNumber(), level.getLocation().getCode(),
                    level.getQtyOnHand());
        }
    }

    public record CreateSalesOrderRequest(
            @NotBlank String customerCode,
            String externalRef,
            @NotEmpty @Valid List<OrderLine> lines) {

        public record OrderLine(
                @NotBlank String partNumber,
                @NotNull @Positive BigDecimal quantity,
                @NotNull @PositiveOrZero BigDecimal unitPrice) { }
    }

    public record SalesOrderDto(Long id, String soNumber, String status, String customerCode, String externalRef,
                                String source, BigDecimal total, List<SalesOrderLineDto> lines) {
        public static SalesOrderDto from(SalesOrder so, BigDecimal total) {
            List<SalesOrderLineDto> lines = so.getLines().stream()
                    .map(l -> new SalesOrderLineDto(l.getLineNo(), l.getPart().getPartNumber(),
                            l.getQtyOrdered(), l.getQtyPicked(), l.getUnitPrice()))
                    .toList();
            return new SalesOrderDto(so.getId(), so.getSoNumber(), so.getStatus().name(),
                    so.getCustomer().getCode(), so.getExternalRef(), so.getSource().name(), total, lines);
        }
    }

    public record SalesOrderLineDto(int lineNo, String partNumber, BigDecimal qtyOrdered, BigDecimal qtyPicked,
                                    BigDecimal unitPrice) { }

    public record ApiError(int status, String error, String message, Instant timestamp) { }
}
