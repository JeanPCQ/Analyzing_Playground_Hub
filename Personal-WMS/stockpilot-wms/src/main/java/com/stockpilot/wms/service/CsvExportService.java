package com.stockpilot.wms.service;

import com.stockpilot.wms.domain.InventoryLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Exports for spreadsheets and other systems. */
@Service
@Transactional(readOnly = true)
public class CsvExportService {

    private final InventoryService inventoryService;

    public CsvExportService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** One row per part per location. Opens in Excel or Numbers. */
    public String inventoryCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("part_number,description,location,zone,qty_on_hand,standard_cost\n");
        List<InventoryLevel> levels = inventoryService.allLevels();
        for (InventoryLevel level : levels) {
            csv.append(String.join(",",
                    level.getPart().getPartNumber(),
                    level.getPart().getDescription(),
                    level.getLocation().getCode(),
                    level.getLocation().getZone(),
                    level.getQtyOnHand().stripTrailingZeros().toPlainString(),
                    level.getPart().getStandardCost().stripTrailingZeros().toPlainString()));
            csv.append("\n");
        }
        return csv.toString();
    }
}
