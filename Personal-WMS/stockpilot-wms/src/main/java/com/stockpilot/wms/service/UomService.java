package com.stockpilot.wms.service;

import com.stockpilot.wms.domain.Uom;
import com.stockpilot.wms.repository.UomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UomService {

    private final UomRepository uomRepository;

    public UomService(UomRepository uomRepository) {
        this.uomRepository = uomRepository;
    }

    public List<Uom> findAll() {
        return uomRepository.findAllByOrderByFactorToEachAsc();
    }

    public Uom get(Long id) {
        return uomRepository.findById(id).orElseThrow(() -> new NotFoundException("Unit of measure not found: " + id));
    }

    /** Converts a quantity in the given unit to eaches. 3 CS -> 36 EA. */
    public BigDecimal toEach(BigDecimal qty, Uom uom) {
        return qty.multiply(uom.getFactorToEach());
    }
}
