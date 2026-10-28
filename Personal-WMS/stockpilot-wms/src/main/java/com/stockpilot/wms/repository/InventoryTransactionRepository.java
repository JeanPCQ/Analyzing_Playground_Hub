package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.InventoryTransaction;
import com.stockpilot.wms.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findTop200ByOrderByIdDesc();
    List<InventoryTransaction> findTop10ByOrderByIdDesc();
    List<InventoryTransaction> findByPartOrderByIdDesc(Part part);
}
