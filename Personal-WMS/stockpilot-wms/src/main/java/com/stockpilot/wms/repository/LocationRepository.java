package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCode(String code);
    List<Location> findAllByOrderByCodeAsc();
    List<Location> findByActiveTrueOrderByCodeAsc();
}
