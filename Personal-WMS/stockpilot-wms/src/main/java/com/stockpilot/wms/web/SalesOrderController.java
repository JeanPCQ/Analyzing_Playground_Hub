package com.stockpilot.wms.web;

import com.stockpilot.wms.domain.InventoryLevel;
import com.stockpilot.wms.domain.OrderSource;
import com.stockpilot.wms.domain.SalesOrder;
import com.stockpilot.wms.domain.SalesOrderLine;
import com.stockpilot.wms.service.InventoryService;
import com.stockpilot.wms.service.MasterDataService;
import com.stockpilot.wms.service.PartService;
import com.stockpilot.wms.service.SalesOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/sales-orders")
public class SalesOrderController {

    private final SalesOrderService soService;
    private final MasterDataService masterData;
    private final PartService partService;
    private final InventoryService inventoryService;

    public SalesOrderController(SalesOrderService soService, MasterDataService masterData,
                                PartService partService, InventoryService inventoryService) {
        this.soService = soService;
        this.masterData = masterData;
        this.partService = partService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", soService.findAll());
        return "sales-orders/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("customers", masterData.allCustomers());
        return "sales-orders/new";
    }

    @PostMapping
    public String create(@RequestParam Long customerId, RedirectAttributes redirect) {
        SalesOrder so = soService.create(customerId, OrderSource.MANUAL, null);
        redirect.addFlashAttribute("success", "Created " + so.getSoNumber() + ". Add lines, then issue it.");
        return "redirect:/sales-orders/" + so.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SalesOrder so = soService.get(id);
        model.addAttribute("so", so);
        model.addAttribute("total", soService.calculateTotal(so));
        model.addAttribute("parts", partService.findAll());
        model.addAttribute("locations", masterData.activeLocations());
        model.addAttribute("binsByLine", binsByLine(so));
        return "sales-orders/detail";
    }

    @PostMapping("/{id}/lines")
    public String addLine(@PathVariable Long id, @RequestParam Long partId, @RequestParam BigDecimal qty,
                          @RequestParam BigDecimal unitPrice, RedirectAttributes redirect) {
        soService.addLine(id, partId, qty, unitPrice);
        redirect.addFlashAttribute("success", "Line added.");
        return "redirect:/sales-orders/" + id;
    }

    @PostMapping("/{id}/lines/{lineId}/delete")
    public String removeLine(@PathVariable Long id, @PathVariable Long lineId, RedirectAttributes redirect) {
        soService.removeLine(id, lineId);
        redirect.addFlashAttribute("success", "Line removed.");
        return "redirect:/sales-orders/" + id;
    }

    @PostMapping("/{id}/issue")
    public String issue(@PathVariable Long id, RedirectAttributes redirect) {
        soService.issue(id);
        redirect.addFlashAttribute("success", "Order issued and stock allocated.");
        return "redirect:/sales-orders/" + id;
    }

    @PostMapping("/{id}/pick")
    public String pick(@PathVariable Long id, @RequestParam Long lineId, @RequestParam Long locationId,
                       @RequestParam BigDecimal qty, RedirectAttributes redirect) {
        soService.pick(id, lineId, locationId, qty);
        redirect.addFlashAttribute("success", "Picked.");
        return "redirect:/sales-orders/" + id;
    }

    @PostMapping("/{id}/ship")
    public String ship(@PathVariable Long id, @RequestParam String carrier,
                       @RequestParam(required = false) String trackingNumber, RedirectAttributes redirect) {
        soService.ship(id, carrier, trackingNumber);
        redirect.addFlashAttribute("success", "Order shipped.");
        return "redirect:/sales-orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        soService.cancel(id);
        redirect.addFlashAttribute("success", "Order cancelled.");
        return "redirect:/sales-orders/" + id;
    }

    /** For each line, the bins that hold that part, so pickers know where to go. */
    private Map<Long, List<InventoryLevel>> binsByLine(SalesOrder so) {
        Map<Long, List<InventoryLevel>> result = new HashMap<>();
        for (SalesOrderLine line : so.getLines()) {
            result.put(line.getId(), inventoryService.levelsFor(line.getPart()));
        }
        return result;
    }
}
