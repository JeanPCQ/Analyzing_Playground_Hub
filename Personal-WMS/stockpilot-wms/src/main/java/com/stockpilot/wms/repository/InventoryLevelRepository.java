package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.InventoryLevel;
import com.stockpilot.wms.domain.Location;
import com.stockpilot.wms.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Long> {

    Optional<InventoryLevel> findByPartAndLocation(Part part, Location location);

    List<InventoryLevel> findByPartOrderByLocationCodeAsc(Part part);

    List<InventoryLevel> findAllByOrderByPartPartNumberAscLocationCodeAsc();

    /** Returns null when the part has no inventory rows. */
    @Query("select sum(i.qtyOnHand) from InventoryLevel i where i.part = :part")
    BigDecimal sumOnHand(@Param("part") Part part);
}
