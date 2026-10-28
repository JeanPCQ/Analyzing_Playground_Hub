package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.PurchaseOrder;
import com.stockpilot.wms.domain.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByIdDesc();
    List<PurchaseOrder> findByStatusInOrderByIdAsc(Collection<PurchaseOrderStatus> statuses);
    long countByStatusIn(Collection<PurchaseOrderStatus> statuses);
}
