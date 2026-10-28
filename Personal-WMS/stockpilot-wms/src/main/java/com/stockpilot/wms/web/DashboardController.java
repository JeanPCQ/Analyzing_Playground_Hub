package com.stockpilot.wms.web;

import com.stockpilot.wms.service.InventoryService;
import com.stockpilot.wms.service.PartService;
import com.stockpilot.wms.service.PartSummary;
import com.stockpilot.wms.service.PurchaseOrderService;
import com.stockpilot.wms.service.SalesOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final PartService partService;
    private final InventoryService inventoryService;
    private final PurchaseOrderService poService;
    private final SalesOrderService soService;

    public DashboardController(PartService partService, InventoryService inventoryService,
                               PurchaseOrderService poService, SalesOrderService soService) {
        this.partService = partService;
        this.inventoryService = inventoryService;
        this.poService = poService;
        this.soService = soService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<PartSummary> lowStock = partService.search(null).stream()
                .filter(s -> s.getPart().isActive() && s.isLowStock())
                .toList();
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("openPoCount", poService.countOpen());
        model.addAttribute("openSoCount", soService.countOpen());
        model.addAttribute("toReceive", poService.openForReceiving());
        model.addAttribute("toPick", soService.openForPicking());
        model.addAttribute("recent", inventoryService.recentTransactions().stream().limit(10).toList());
        return "dashboard";
    }
}
