package com.stockpilot.wms.repository;

import com.stockpilot.wms.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCode(String code);
    boolean existsByCode(String code);
    List<Customer> findAllByOrderByNameAsc();
}
