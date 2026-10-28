package com.stockpilot.wms.api;

import com.stockpilot.wms.api.ApiDtos.CreateSalesOrderRequest;
import com.stockpilot.wms.api.ApiDtos.SalesOrderDto;
import com.stockpilot.wms.domain.SalesOrder;
import com.stockpilot.wms.service.OrderLineRequest;
import com.stockpilot.wms.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-orders")
@Tag(name = "Sales orders")
public class SalesOrderApiController {

    private final SalesOrderService soService;

    public SalesOrderApiController(SalesOrderService soService) {
        this.soService = soService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create and issue a sales order (used by the web store connector)")
    public SalesOrderDto create(@Valid @RequestBody CreateSalesOrderRequest request) {
        List<OrderLineRequest> lines = request.lines().stream()
                .map(l -> new OrderLineRequest(l.partNumber().trim().toUpperCase(), l.quantity(), l.unitPrice()))
                .toList();
        SalesOrder so = soService.createFromApi(request.customerCode().trim().toUpperCase(),
                request.externalRef(), lines);
        SalesOrder saved = soService.get(so.getId());
        return SalesOrderDto.from(saved, soService.calculateTotal(saved));
    }

    @GetMapping("/{soNumber}")
    public SalesOrderDto get(@PathVariable String soNumber) {
        SalesOrder so = soService.getByNumber(soNumber);
        return SalesOrderDto.from(so, soService.calculateTotal(so));
    }
}
