package com.stockpilot.wms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Unit of measure. Every quantity in inventory is stored in EA (each). */
@Entity
@Table(name = "uom")
public class Uom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    /** How many EA are in one of this unit. */
    @Column(name = "factor_to_each", nullable = false, precision = 19, scale = 4)
    private BigDecimal factorToEach;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getFactorToEach() { return factorToEach; }
    public void setFactorToEach(BigDecimal factorToEach) { this.factorToEach = factorToEach; }
}
