package com.stockpilot.wms.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", unique = true, length = 20)
    private String poNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate = LocalDate.now();

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("lineNo ASC")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public boolean isEditable() {
        return status == PurchaseOrderStatus.DRAFT;
    }

    /** Fully received or cancelled: nothing more can happen to this order. */
    public boolean isClosed() {
        return status == PurchaseOrderStatus.RECEIVED || status == PurchaseOrderStatus.CANCELLED;
    }

    public boolean isReceivable() {
        return status == PurchaseOrderStatus.ISSUED || status == PurchaseOrderStatus.PARTIALLY_RECEIVED;
    }

    public Long getId() { return id; }
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public PurchaseOrderStatus getStatus() { return status; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<PurchaseOrderLine> getLines() { return lines; }
}
