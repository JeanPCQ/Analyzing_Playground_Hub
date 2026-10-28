package com.stockpilot.wms.web;

import com.stockpilot.wms.domain.InventoryLevel;
import com.stockpilot.wms.domain.Part;
import com.stockpilot.wms.domain.SalesOrder;
import com.stockpilot.wms.domain.SalesOrderLine;
import com.stockpilot.wms.repository.PartRepository;
import com.stockpilot.wms.service.InventoryService;
import com.stockpilot.wms.service.MasterDataService;
import com.stockpilot.wms.service.PartService;
import com.stockpilot.wms.service.PurchaseOrderService;
import com.stockpilot.wms.service.SalesOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Warehouse floor screens: big buttons, one task per screen, sized for a phone or scanner.
 * Open http://<your-Mac's-IP>:8080/floor on a phone on the same Wi-Fi.
 */
@Controller
@RequestMapping("/floor")
public class FloorController {

    private final PurchaseOrderService poService;
    private final SalesOrderService soService;
    private final InventoryService inventoryService;
    private final MasterDataService masterData;
    private final PartService partService;
    private final PartRepository partRepository;

    public FloorController(PurchaseOrderService poService, SalesOrderService soService,
                           InventoryService inventoryService, MasterDataService masterData,
                           PartService partService, PartRepository partRepository) {
        this.poService = poService;
        this.soService = soService;
        this.inventoryService = inventoryService;
        this.masterData = masterData;
        this.partService = partService;
        this.partRepository = partRepository;
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("receiveCount", poService.openForReceiving().size());
        model.addAttribute("pickCount", soService.openForPicking().size());
        return "floor/home";
    }

    // ---- Receiving ----

    @GetMapping("/receive")
    public String receiveList(Model model) {
        model.addAttribute("orders", poService.openForReceiving());
        return "floor/receive-list";
    }

    @GetMapping("/receive/{poId}")
    public String receivePo(@PathVariable Long poId, Model model) {
        model.addAttribute("po", poService.get(poId));
        model.addAttribute("locations", masterData.activeLocations());
        return "floor/receive-po";
    }

    @PostMapping("/receive/{poId}")
    public String receive(@PathVariable Long poId, @RequestParam Long lineId, @RequestParam BigDecimal qty,
                          @RequestParam Long locationId, RedirectAttributes redirect) {
        poService.receive(poId, lineId, qty, locationId);
        redirect.addFlashAttribute("success", "Received " + qty.stripTrailingZeros().toPlainString() + ".");
        return "redirect:/floor/receive/" + poId;
    }

    // ---- Picking ----

    @GetMapping("/pick")
    public String pickList(Model model) {
        model.addAttribute("orders", soService.openForPicking());
        return "floor/pick-list";
    }

    @GetMapping("/pick/{soId}")
    public String pickSo(@PathVariable Long soId, Model model) {
        SalesOrder so = soService.get(soId);
        Map<Long, List<InventoryLevel>> bins = new HashMap<>();
        for (SalesOrderLine line : so.getLines()) {
            bins.put(line.getId(), inventoryService.levelsFor(line.getPart()));
        }
        model.addAttribute("so", so);
        model.addAttribute("binsByLine", bins);
        return "floor/pick-so";
    }

    @PostMapping("/pick/{soId}")
    public String pick(@PathVariable Long soId, @RequestParam Long lineId, @RequestParam Long locationId,
                       @RequestParam BigDecimal qty, RedirectAttributes redirect) {
        soService.pick(soId, lineId, locationId, qty);
        redirect.addFlashAttribute("success", "Picked " + qty.stripTrailingZeros().toPlainString() + ".");
        return "redirect:/floor/pick/" + soId;
    }

    // ---- Item lookup ----

    @GetMapping("/lookup")
    public String lookup(@RequestParam(required = false) String partNumber, Model model) {
        model.addAttribute("partNumber", partNumber);
        if (partNumber != null && !partNumber.isBlank()) {
            Optional<Part> part = partRepository.findByPartNumber(partNumber.trim().toUpperCase());
            if (part.isPresent()) {
                model.addAttribute("summary", partService.summarize(part.get()));
                model.addAttribute("levels", inventoryService.levelsFor(part.get()));
            } else {
                model.addAttribute("notFound", true);
            }
        }
        return "floor/lookup";
    }
}
