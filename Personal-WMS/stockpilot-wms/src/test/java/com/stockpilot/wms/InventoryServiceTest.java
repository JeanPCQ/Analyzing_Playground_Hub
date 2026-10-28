package com.stockpilot.wms;

import com.stockpilot.wms.domain.Location;
import com.stockpilot.wms.domain.Part;
import com.stockpilot.wms.service.BusinessException;
import com.stockpilot.wms.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired InventoryService inventoryService;
    @Autowired TestData data;

    @Test
    @DisplayName("Adjustments add stock and are recorded in history")
    void adjustAddsStockAndLogsIt() {
        Part part = data.part("ADJ");
        Location bin = data.location("BIN");

        inventoryService.adjust(part.getId(), bin.getId(), new BigDecimal("25"), "Cycle count found extra");

        assertThat(inventoryService.onHandAt(part, bin)).isEqualByComparingTo("25");
        assertThat(inventoryService.transactionsFor(part)).hasSize(1);
    }

    @Test
    @DisplayName("An adjustment can't make a location negative")
    void adjustCannotGoNegative() {
        Part part = data.part("ADJ");
        Location bin = data.location("BIN");
        inventoryService.adjust(part.getId(), bin.getId(), new BigDecimal("5"), "Initial");

        assertThatThrownBy(() -> inventoryService.adjust(part.getId(), bin.getId(), new BigDecimal("-6"), "Damaged"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Transfers move stock between locations")
    void transferMovesStock() {
        Part part = data.part("XFR");
        Location from = data.location("FROM");
        Location to = data.location("TO");
        inventoryService.adjust(part.getId(), from.getId(), new BigDecimal("10"), "Initial");

        inventoryService.transfer(part.getId(), from.getId(), to.getId(), new BigDecimal("4"));

        assertThat(inventoryService.onHandAt(part, from)).isEqualByComparingTo("6");
        assertThat(inventoryService.onHandAt(part, to)).isEqualByComparingTo("4");
        assertThat(inventoryService.totalOnHand(part)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Can't transfer more than the source location holds")
    void transferChecksSourceLocation() {
        Part part = data.part("XFR");
        Location from = data.location("FROM");
        Location other = data.location("OTHER");
        Location to = data.location("TO");
        inventoryService.adjust(part.getId(), from.getId(), new BigDecimal("3"), "Initial");
        inventoryService.adjust(part.getId(), other.getId(), new BigDecimal("50"), "Initial");

        assertThatThrownBy(() -> inventoryService.transfer(part.getId(), from.getId(), to.getId(), new BigDecimal("4")))
                .isInstanceOf(BusinessException.class);
    }
}
