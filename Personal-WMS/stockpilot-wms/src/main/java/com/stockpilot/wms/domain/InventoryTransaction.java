package com.stockpilot.wms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Audit trail: one row for every change to an inventory level. Never updated or deleted. */
@Entity
@Table(name = "inventory_transaction")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "qty_change", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyChange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(length = 100)
    private String reference;

    private String note;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected InventoryTransaction() { }

    public InventoryTransaction(Part part, Location location, BigDecimal qtyChange, TransactionType type,
                                String reference, String note, String username) {
        this.part = part;
        this.location = location;
        this.qtyChange = qtyChange;
        this.type = type;
        this.reference = reference;
        this.note = note;
        this.username = username;
    }

    public Long getId() { return id; }
    public Part getPart() { return part; }
    public Location getLocation() { return location; }
    public BigDecimal getQtyChange() { return qtyChange; }
    public TransactionType getType() { return type; }
    public String getReference() { return reference; }
    public String getNote() { return note; }
    public String getUsername() { return username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
