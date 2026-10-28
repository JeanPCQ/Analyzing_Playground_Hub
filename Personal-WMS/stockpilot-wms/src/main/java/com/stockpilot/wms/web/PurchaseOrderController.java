package com.stockpilot.wms.web;

import com.stockpilot.wms.domain.PurchaseOrder;
import com.stockpilot.wms.service.MasterDataService;
import com.stockpilot.wms.service.PartService;
import com.stockpilot.wms.service.PurchaseOrderService;
import com.stockpilot.wms.service.UomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;
    private final MasterDataService masterData;
    private final PartService partService;
    private final UomService uomService;

    public PurchaseOrderController(PurchaseOrderService poService, MasterDataService masterData,
                                   PartService partService, UomService uomService) {
        this.poService = poService;
        this.masterData = masterData;
        this.partService = partService;
        this.uomService = uomService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", poService.findAll());
        return "purchase-orders/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("vendors", masterData.allVendors());
        return "purchase-orders/new";
    }

    @PostMapping
    public String create(@RequestParam Long vendorId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDate,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes redirect) {
        PurchaseOrder po = poService.create(vendorId, expectedDate, notes);
        redirect.addFlashAttribute("success", "Created " + po.getPoNumber() + ". Add lines, then issue it.");
        return "redirect:/purchase-orders/" + po.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PurchaseOrder po = poService.get(id);
        model.addAttribute("po", po);
        model.addAttribute("total", poService.calculateTotal(po));
        model.addAttribute("parts", partService.findAll());
        model.addAttribute("uoms", uomService.findAll());
        model.addAttribute("locations", masterData.activeLocations());
        return "purchase-orders/detail";
    }

    @PostMapping("/{id}/lines")
    public String addLine(@PathVariable Long id, @RequestParam Long partId, @RequestParam Long uomId,
                          @RequestParam BigDecimal qty, @RequestParam BigDecimal unitCost,
                          RedirectAttributes redirect) {
        poService.addLine(id, partId, uomId, qty, unitCost);
        redirect.addFlashAttribute("success", "Line added.");
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/lines/{lineId}/delete")
    public String removeLine(@PathVariable Long id, @PathVariable Long lineId, RedirectAttributes redirect) {
        poService.removeLine(id, lineId);
        redirect.addFlashAttribute("success", "Line removed.");
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/issue")
    public String issue(@PathVariable Long id, RedirectAttributes redirect) {
        poService.issue(id);
        redirect.addFlashAttribute("success", "Purchase order issued. It can now be received.");
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        poService.cancel(id);
        redirect.addFlashAttribute("success", "Purchase order cancelled.");
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/receive")
    public String receive(@PathVariable Long id, @RequestParam Long lineId, @RequestParam BigDecimal qty,
                          @RequestParam Long locationId, RedirectAttributes redirect) {
        poService.receive(id, lineId, qty, locationId);
        redirect.addFlashAttribute("success", "Received.");
        return "redirect:/purchase-orders/" + id;
    }
}
