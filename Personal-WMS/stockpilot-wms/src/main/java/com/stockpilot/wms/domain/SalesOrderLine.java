package com.stockpilot.wms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** One line on a sales order. Quantities are in EA. */
@Entity
@Table(name = "sales_order_line")
public class SalesOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    @Column(name = "qty_ordered", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyOrdered;

    @Column(name = "qty_picked", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyPicked = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    public BigDecimal getQtyRemaining() {
        return qtyOrdered.subtract(qtyPicked);
    }

    public boolean isFullyPicked() {
        return qtyPicked.compareTo(qtyOrdered) >= 0;
    }

    public BigDecimal getLineTotal() {
        return qtyOrdered.multiply(unitPrice);
    }

    public Long getId() { return id; }
    public SalesOrder getSalesOrder() { return salesOrder; }
    public void setSalesOrder(SalesOrder salesOrder) { this.salesOrder = salesOrder; }
    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }
    public BigDecimal getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }
    public BigDecimal getQtyPicked() { return qtyPicked; }
    public void setQtyPicked(BigDecimal qtyPicked) { this.qtyPicked = qtyPicked; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
