package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.SalesOrder;
import com.stockpilot.wms.domain.SalesOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    List<SalesOrder> findAllByOrderByIdDesc();
    List<SalesOrder> findByStatusInOrderByIdAsc(Collection<SalesOrderStatus> statuses);
    long countByStatusIn(Collection<SalesOrderStatus> statuses);
    Optional<SalesOrder> findBySoNumber(String soNumber);
}
