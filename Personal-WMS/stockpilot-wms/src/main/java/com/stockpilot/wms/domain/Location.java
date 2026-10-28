package com.stockpilot.wms.domain;

import jakarta.persistence.*;

/** A place where stock can sit: a dock, a bin, a staging area. */
@Entity
@Table(name = "location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationType type;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public LocationType getType() { return type; }
    public void setType(LocationType type) { this.type = type; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
