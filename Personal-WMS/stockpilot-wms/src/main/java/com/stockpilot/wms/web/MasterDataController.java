package com.stockpilot.wms.web;

import com.stockpilot.wms.domain.LocationType;
import com.stockpilot.wms.service.MasterDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Locations, vendors and customers: list on one page, add with a small form. */
@Controller
public class MasterDataController {

    private final MasterDataService masterData;

    public MasterDataController(MasterDataService masterData) {
        this.masterData = masterData;
    }

    @GetMapping("/locations")
    public String locations(Model model) {
        model.addAttribute("locations", masterData.allLocations());
        model.addAttribute("types", LocationType.values());
        return "locations";
    }

    @PostMapping("/locations")
    public String createLocation(@RequestParam String code, @RequestParam String name,
                                 @RequestParam String zone, @RequestParam LocationType type,
                                 RedirectAttributes redirect) {
        masterData.createLocation(code, name, zone, type);
        redirect.addFlashAttribute("success", "Added location " + code.trim().toUpperCase() + ".");
        return "redirect:/locations";
    }

    @PostMapping("/locations/{id}/toggle")
    public String toggleLocation(@PathVariable Long id, RedirectAttributes redirect) {
        masterData.toggleLocation(id);
        redirect.addFlashAttribute("success", "Location updated.");
        return "redirect:/locations";
    }

    @GetMapping("/vendors")
    public String vendors(Model model) {
        model.addAttribute("vendors", masterData.allVendors());
        return "vendors";
    }

    @PostMapping("/vendors")
    public String createVendor(@RequestParam String code, @RequestParam String name,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String phone,
                               RedirectAttributes redirect) {
        masterData.createVendor(code, name, email, phone);
        redirect.addFlashAttribute("success", "Added vendor " + name.trim() + ".");
        return "redirect:/vendors";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", masterData.allCustomers());
        return "customers";
    }

    @PostMapping("/customers")
    public String createCustomer(@RequestParam String code, @RequestParam String name,
                                 @RequestParam(required = false) String email,
                                 @RequestParam(required = false) String shipTo,
                                 RedirectAttributes redirect) {
        masterData.createCustomer(code, name, email, shipTo);
        redirect.addFlashAttribute("success", "Added customer " + name.trim() + ".");
        return "redirect:/customers";
    }
}
