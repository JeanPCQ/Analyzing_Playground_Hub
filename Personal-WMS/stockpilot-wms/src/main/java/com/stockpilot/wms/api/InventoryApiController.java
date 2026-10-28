package com.stockpilot.wms.api;

import com.stockpilot.wms.api.ApiDtos.InventoryLevelDto;
import com.stockpilot.wms.service.InventoryService;
import com.stockpilot.wms.service.PartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory")
public class InventoryApiController {

    private final InventoryService inventoryService;
    private final PartService partService;

    public InventoryApiController(InventoryService inventoryService, PartService partService) {
        this.inventoryService = inventoryService;
        this.partService = partService;
    }

    /** All inventory by location, or just one part's with ?partNumber=GLV-L */
    @GetMapping
    public List<InventoryLevelDto> levels(@RequestParam(required = false) String partNumber) {
        var levels = (partNumber == null || partNumber.isBlank())
                ? inventoryService.allLevels()
                : inventoryService.levelsFor(partService.getByNumber(partNumber));
        return levels.stream().map(InventoryLevelDto::from).toList();
    }
}
