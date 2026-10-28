package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    boolean existsByCode(String code);
    List<Vendor> findAllByOrderByNameAsc();
}
