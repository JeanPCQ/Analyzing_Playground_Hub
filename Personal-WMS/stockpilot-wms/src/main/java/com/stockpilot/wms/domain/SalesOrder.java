package com.stockpilot.wms.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_order")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "so_number", unique = true, length = 20)
    private String soNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate = LocalDate.now();

    /** The order number in the system that sent it (e.g. the web store). */
    @Column(name = "external_ref", length = 60)
    private String externalRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderSource source = OrderSource.MANUAL;

    @Column(length = 40)
    private String carrier;

    @Column(name = "tracking_number", length = 60)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("lineNo ASC")
    private List<SalesOrderLine> lines = new ArrayList<>();

    public boolean isEditable() {
        return status == SalesOrderStatus.DRAFT;
    }

    public boolean isPicked() {
        return status == SalesOrderStatus.PICKED;
    }

    /** Shipped or cancelled: nothing more can happen to this order. */
    public boolean isClosed() {
        return status == SalesOrderStatus.SHIPPED || status == SalesOrderStatus.CANCELLED;
    }

    public boolean isPickable() {
        return status == SalesOrderStatus.ISSUED || status == SalesOrderStatus.IN_PICKING;
    }

    public Long getId() { return id; }
    public String getSoNumber() { return soNumber; }
    public void setSoNumber(String soNumber) { this.soNumber = soNumber; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public SalesOrderStatus getStatus() { return status; }
    public void setStatus(SalesOrderStatus status) { this.status = status; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }
    public OrderSource getSource() { return source; }
    public void setSource(OrderSource source) { this.source = source; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<SalesOrderLine> getLines() { return lines; }
}
