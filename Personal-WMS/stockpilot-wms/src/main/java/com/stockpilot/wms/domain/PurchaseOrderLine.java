package com.stockpilot.wms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** One line on a PO. Quantities here are in the line's UOM (e.g. cases), not EA. */
@Entity
@Table(name = "purchase_order_line")
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uom_id")
    private Uom uom;

    @Column(name = "qty_ordered", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyOrdered;

    @Column(name = "qty_received", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyReceived = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PoLineStatus status = PoLineStatus.OPEN;

    public BigDecimal getQtyRemaining() {
        return qtyOrdered.subtract(qtyReceived);
    }

    public BigDecimal getLineTotal() {
        return qtyOrdered.multiply(unitCost);
    }

    public Long getId() { return id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }
    public Uom getUom() { return uom; }
    public void setUom(Uom uom) { this.uom = uom; }
    public BigDecimal getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }
    public BigDecimal getQtyReceived() { return qtyReceived; }
    public void setQtyReceived(BigDecimal qtyReceived) { this.qtyReceived = qtyReceived; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public PoLineStatus getStatus() { return status; }
    public void setStatus(PoLineStatus status) { this.status = status; }
}
