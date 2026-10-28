package com.stockpilot.wms;

import com.stockpilot.wms.domain.*;
import com.stockpilot.wms.service.BusinessException;
import com.stockpilot.wms.service.InventoryService;
import com.stockpilot.wms.service.PurchaseOrderService;
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
class PurchaseOrderServiceTest {

    @Autowired PurchaseOrderService poService;
    @Autowired InventoryService inventoryService;
    @Autowired TestData data;

    private PurchaseOrder issuedPo(Part part, Uom uom, String qty, String unitCost) {
        PurchaseOrder po = poService.create(data.vendor().getId(), null, null);
        poService.addLine(po.getId(), part.getId(), uom.getId(), new BigDecimal(qty), new BigDecimal(unitCost));
        poService.issue(po.getId());
        return po;
    }

    @Test
    @DisplayName("Receiving the full quantity in one go closes the PO")
    void receivingFullQuantityClosesTheOrder() {
        Part part = data.part("RCV");
        Location bin = data.location("BIN");
        PurchaseOrder po = issuedPo(part, data.each(), "10", "2.00");

        poService.receive(po.getId(), po.getLines().get(0).getId(), new BigDecimal("10"), bin.getId());

        assertThat(poService.get(po.getId()).getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(inventoryService.onHandAt(part, bin)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Receiving cases puts eaches into inventory")
    void receivingCasesStoresEaches() {
        Part part = data.part("CASE");
        Location bin = data.location("BIN");
        PurchaseOrder po = issuedPo(part, data.caseOf12(), "2", "24.00");

        poService.receive(po.getId(), po.getLines().get(0).getId(), new BigDecimal("2"), bin.getId());

        assertThat(inventoryService.onHandAt(part, bin)).isEqualByComparingTo("24");
    }

    @Test
    @DisplayName("Can't receive more than what's left on the line")
    void cannotReceiveMoreThanOrdered() {
        Part part = data.part("OVER");
        Location bin = data.location("BIN");
        PurchaseOrder po = issuedPo(part, data.each(), "5", "1.00");
        Long lineId = po.getLines().get(0).getId();

        assertThatThrownBy(() -> poService.receive(po.getId(), lineId, new BigDecimal("6"), bin.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("WMS-102: a line received in two partial receipts closes the PO")
    void partialReceiptsThatCompleteTheLineCloseTheOrder() {
        Part part = data.part("PART");
        Location bin = data.location("BIN");
        PurchaseOrder po = issuedPo(part, data.each(), "10", "1.00");
        Long lineId = po.getLines().get(0).getId();

        poService.receive(po.getId(), lineId, new BigDecimal("6"), bin.getId());
        assertThat(poService.get(po.getId()).getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        poService.receive(po.getId(), lineId, new BigDecimal("4"), bin.getId());
        assertThat(poService.get(po.getId()).getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
    }

    @Test
    @DisplayName("WMS-101: the order total is exact to the cent")
    void orderTotalIsExact() {
        PurchaseOrder po = poService.create(data.vendor().getId(), null, null);
        poService.addLine(po.getId(), data.part("ZIP").getId(), data.each().getId(), new BigDecimal("10"), new BigDecimal("0.10"));
        poService.addLine(po.getId(), data.part("CLP").getId(), data.each().getId(), new BigDecimal("3"), new BigDecimal("1.15"));

        BigDecimal total = poService.calculateTotal(poService.get(po.getId()));

        assertThat(total).isEqualByComparingTo("4.45");
    }
}
