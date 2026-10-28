package com.stockpilot.wms.web;

import com.stockpilot.wms.service.CsvExportService;
import com.stockpilot.wms.service.InventoryService;
import com.stockpilot.wms.service.MasterDataService;
import com.stockpilot.wms.service.PartService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final PartService partService;
    private final MasterDataService masterData;
    private final CsvExportService csvExportService;

    public InventoryController(InventoryService inventoryService, PartService partService,
                               MasterDataService masterData, CsvExportService csvExportService) {
        this.inventoryService = inventoryService;
        this.partService = partService;
        this.masterData = masterData;
        this.csvExportService = csvExportService;
    }

    @GetMapping
    public String levels(Model model) {
        model.addAttribute("levels", inventoryService.allLevels());
        model.addAttribute("parts", partService.findAll());
        model.addAttribute("locations", masterData.activeLocations());
        return "inventory/levels";
    }

    @PostMapping("/adjust")
    public String adjust(@RequestParam Long partId, @RequestParam Long locationId,
                         @RequestParam BigDecimal qtyChange, @RequestParam String reason,
                         RedirectAttributes redirect) {
        inventoryService.adjust(partId, locationId, qtyChange, reason);
        redirect.addFlashAttribute("success", "Adjustment saved.");
        return "redirect:/inventory";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam Long partId, @RequestParam Long fromLocationId,
                           @RequestParam Long toLocationId, @RequestParam BigDecimal qty,
                           RedirectAttributes redirect) {
        inventoryService.transfer(partId, fromLocationId, toLocationId, qty);
        redirect.addFlashAttribute("success", "Transfer saved.");
        return "redirect:/inventory";
    }

    @GetMapping("/transactions")
    public String transactions(Model model) {
        model.addAttribute("transactions", inventoryService.recentTransactions());
        return "inventory/transactions";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] body = csvExportService.inventoryCsv().getBytes(StandardCharsets.UTF_8);
        String filename = "inventory-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
