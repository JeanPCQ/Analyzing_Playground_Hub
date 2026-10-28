package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByPartNumber(String partNumber);
    boolean existsByPartNumber(String partNumber);
    List<Part> findAllByOrderByPartNumberAsc();
    List<Part> findByPartNumberContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPartNumberAsc(
            String partNumber, String description);
}
