package com.stockpilot.wms;

import com.stockpilot.wms.domain.*;
import com.stockpilot.wms.service.BusinessException;
import com.stockpilot.wms.service.InventoryService;
import com.stockpilot.wms.service.PartService;
import com.stockpilot.wms.service.SalesOrderService;
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
class SalesOrderServiceTest {

    @Autowired SalesOrderService soService;
    @Autowired InventoryService inventoryService;
    @Autowired PartService partService;
    @Autowired TestData data;

    private SalesOrder draftOrder(Part part, String qty) {
        SalesOrder so = soService.create(data.customer().getId(), OrderSource.MANUAL, null);
        soService.addLine(so.getId(), part.getId(), new BigDecimal(qty), new BigDecimal("5.00"));
        return so;
    }

    @Test
    @DisplayName("Issuing an order allocates its quantity")
    void issueAllocatesStock() {
        Part part = data.part("ALLOC");
        Location bin = data.location("BIN");
        inventoryService.adjust(part.getId(), bin.getId(), new BigDecimal("20"), "Initial");
        SalesOrder so = draftOrder(part, "8");

        soService.issue(so.getId());

        assertThat(partService.summarize(partService.get(part.getId())).getAvailable()).isEqualByComparingTo("12");
    }

    @Test
    @DisplayName("Can't issue an order for more than is available")
    void cannotIssueMoreThanAvailable() {
        Part part = data.part("SHORT");
        Location bin = data.location("BIN");
        inventoryService.adjust(part.getId(), bin.getId(), new BigDecimal("3"), "Initial");
        SalesOrder so = draftOrder(part, "4");

        assertThatThrownBy(() -> soService.issue(so.getId())).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Picking removes stock, releases the allocation and completes the order")
    void pickingCompletesTheOrder() {
        Part part = data.part("PICK");
        Location bin = data.location("BIN");
        inventoryService.adjust(part.getId(), bin.getId(), new BigDecimal("10"), "Initial");
        SalesOrder so = draftOrder(part, "6");
        soService.issue(so.getId());
        Long lineId = soService.get(so.getId()).getLines().get(0).getId();

        soService.pick(so.getId(), lineId, bin.getId(), new BigDecimal("6"));

        assertThat(soService.get(so.getId()).getStatus()).isEqualTo(SalesOrderStatus.PICKED);
        assertThat(inventoryService.onHandAt(part, bin)).isEqualByComparingTo("4");
        assertThat(partService.get(part.getId()).getQtyAllocated()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Shipping a picked order records the carrier")
    void shipPickedOrder() {
        Part part = data.part("SHIP");
        Location bin = data.location("BIN");
        inventoryService.adjust(part.getId(), bin.getId(), new BigDecimal("2"), "Initial");
        SalesOrder so = draftOrder(part, "2");
        soService.issue(so.getId());
        soService.pick(so.getId(), soService.get(so.getId()).getLines().get(0).getId(), bin.getId(), new BigDecimal("2"));

        soService.ship(so.getId(), "UPS", "1Z999");

        SalesOrder shipped = soService.get(so.getId());
        assertThat(shipped.getStatus()).isEqualTo(SalesOrderStatus.SHIPPED);
        assertThat(shipped.getCarrier()).isEqualTo("UPS");
    }
}
