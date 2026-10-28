package com.stockpilot.wms.web;

import com.stockpilot.wms.domain.Part;
import com.stockpilot.wms.service.BusinessException;
import com.stockpilot.wms.service.InventoryService;
import com.stockpilot.wms.service.PartService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/parts")
public class PartController {

    private final PartService partService;
    private final InventoryService inventoryService;

    public PartController(PartService partService, InventoryService inventoryService) {
        this.partService = partService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("parts", partService.search(q));
        return "parts/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new PartForm());
        model.addAttribute("editing", false);
        return "parts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") PartForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            return "parts/form";
        }
        try {
            Part part = partService.create(form.getPartNumber(), form.getDescription(),
                    form.getStandardCost(), form.getReorderPoint());
            redirect.addFlashAttribute("success", "Created part " + part.getPartNumber() + ".");
            return "redirect:/parts/" + part.getId();
        } catch (BusinessException e) {
            result.rejectValue("partNumber", "duplicate", e.getMessage());
            model.addAttribute("editing", false);
            return "parts/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Part part = partService.get(id);
        model.addAttribute("summary", partService.summarize(part));
        model.addAttribute("levels", inventoryService.levelsFor(part));
        model.addAttribute("transactions", inventoryService.transactionsFor(part));
        return "parts/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Part part = partService.get(id);
        PartForm form = new PartForm();
        form.setPartNumber(part.getPartNumber());
        form.setDescription(part.getDescription());
        form.setStandardCost(part.getStandardCost());
        form.setReorderPoint(part.getReorderPoint());
        form.setActive(part.isActive());
        model.addAttribute("form", form);
        model.addAttribute("partId", id);
        model.addAttribute("editing", true);
        return "parts/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") PartForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("editing", true);
            return "parts/form";
        }
        partService.update(id, form.getDescription(), form.getStandardCost(), form.getReorderPoint(), form.isActive());
        redirect.addFlashAttribute("success", "Saved changes.");
        return "redirect:/parts/" + id;
    }
}
