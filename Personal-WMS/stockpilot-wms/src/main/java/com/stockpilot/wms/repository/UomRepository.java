package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.Uom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UomRepository extends JpaRepository<Uom, Long> {
    Optional<Uom> findByCode(String code);
    List<Uom> findAllByOrderByFactorToEachAsc();
}
