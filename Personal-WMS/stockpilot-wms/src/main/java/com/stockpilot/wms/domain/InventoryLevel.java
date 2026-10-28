package com.stockpilot.wms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** How much of one part sits in one location, in EA. */
@Entity
@Table(name = "inventory_level",
       uniqueConstraints = @UniqueConstraint(columnNames = {"part_id", "location_id"}))
public class InventoryLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "qty_on_hand", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyOnHand = BigDecimal.ZERO;

    protected InventoryLevel() { }

    public InventoryLevel(Part part, Location location) {
        this.part = part;
        this.location = location;
    }

    public Long getId() { return id; }
    public Part getPart() { return part; }
    public Location getLocation() { return location; }
    public BigDecimal getQtyOnHand() { return qtyOnHand; }
    public void setQtyOnHand(BigDecimal qtyOnHand) { this.qtyOnHand = qtyOnHand; }
}
