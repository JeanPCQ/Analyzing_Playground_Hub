package com.stockpilot.wms.service;

import com.stockpilot.wms.domain.*;
import com.stockpilot.wms.repository.CustomerRepository;
import com.stockpilot.wms.repository.LocationRepository;
import com.stockpilot.wms.repository.PartRepository;
import com.stockpilot.wms.repository.SalesOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Sales order lifecycle: DRAFT -> ISSUED (stock allocated) -> IN_PICKING -> PICKED -> SHIPPED.
 * Part.qtyAllocated is the running total of quantity promised to open orders.
 */
@Service
@Transactional
public class SalesOrderService {

    private static final Logger log = LoggerFactory.getLogger(SalesOrderService.class);

    private final SalesOrderRepository soRepository;
    private final CustomerRepository customerRepository;
    private final PartRepository partRepository;
    private final LocationRepository locationRepository;
    private final InventoryService inventoryService;

    public SalesOrderService(SalesOrderRepository soRepository, CustomerRepository customerRepository,
                             PartRepository partRepository, LocationRepository locationRepository,
                             InventoryService inventoryService) {
        this.soRepository = soRepository;
        this.customerRepository = customerRepository;
        this.partRepository = partRepository;
        this.locationRepository = locationRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<SalesOrder> findAll() {
        return soRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<SalesOrder> openForPicking() {
        return soRepository.findByStatusInOrderByIdAsc(EnumSet.of(SalesOrderStatus.ISSUED, SalesOrderStatus.IN_PICKING));
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return soRepository.countByStatusIn(EnumSet.of(SalesOrderStatus.DRAFT, SalesOrderStatus.ISSUED,
                SalesOrderStatus.IN_PICKING, SalesOrderStatus.PICKED));
    }

    @Transactional(readOnly = true)
    public SalesOrder get(Long id) {
        return soRepository.findById(id).orElseThrow(() -> new NotFoundException("Sales order not found: " + id));
    }

    @Transactional(readOnly = true)
    public SalesOrder getByNumber(String soNumber) {
        return soRepository.findBySoNumber(soNumber)
                .orElseThrow(() -> new NotFoundException("Sales order not found: " + soNumber));
    }

    public SalesOrder create(Long customerId, OrderSource source, String externalRef) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
        SalesOrder so = new SalesOrder();
        so.setCustomer(customer);
        so.setSource(source);
        so.setExternalRef(externalRef);
        so = soRepository.save(so);
        so.setSoNumber(String.format("SO-%05d", so.getId()));
        log.info("Created {} for customer {} (source {})", so.getSoNumber(), customer.getCode(), source);
        return so;
    }

    public void addLine(Long soId, Long partId, BigDecimal qty, BigDecimal unitPrice) {
        SalesOrder so = get(soId);
        if (!so.isEditable()) {
            throw new BusinessException("Lines can only be added while the order is a draft.");
        }
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("Quantity must be greater than zero.");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new BusinessException("Unit price can't be negative.");
        }
        Part part = partRepository.findById(partId).orElseThrow(() -> new NotFoundException("Part not found: " + partId));
        SalesOrderLine line = new SalesOrderLine();
        line.setSalesOrder(so);
        line.setLineNo(so.getLines().size() + 1);
        line.setPart(part);
        line.setQtyOrdered(qty);
        line.setUnitPrice(unitPrice);
        so.getLines().add(line);
        soRepository.flush(); // write the new line now so it gets its ID
    }

    public void removeLine(Long soId, Long lineId) {
        SalesOrder so = get(soId);
        if (!so.isEditable()) {
            throw new BusinessException("Lines can only be removed while the order is a draft.");
        }
        so.getLines().removeIf(l -> l.getId().equals(lineId));
        int n = 1;
        for (SalesOrderLine l : so.getLines()) {
            l.setLineNo(n++);
        }
    }

    /** Issue the order and allocate stock for every line. */
    public void issue(Long soId) {
        SalesOrder so = get(soId);
        if (so.getStatus() != SalesOrderStatus.DRAFT) {
            throw new BusinessException("Only draft orders can be issued.");
        }
        if (so.getLines().isEmpty()) {
            throw new BusinessException("Add at least one line before issuing.");
        }
        for (SalesOrderLine line : so.getLines()) {
            Part part = line.getPart();
            BigDecimal available = inventoryService.totalOnHand(part).subtract(part.getQtyAllocated());
            if (available.compareTo(line.getQtyOrdered()) < 0) {
                throw new BusinessException("Not enough " + part.getPartNumber() + " available (available: "
                        + available.stripTrailingZeros().toPlainString() + ").");
            }
            part.setQtyAllocated(part.getQtyAllocated().add(line.getQtyOrdered()));
        }
        so.setStatus(SalesOrderStatus.ISSUED);
        log.info("Issued {}", so.getSoNumber());
    }

    /** Pick qty of one line from one location. */
    public void pick(Long soId, Long lineId, Long locationId, BigDecimal qty) {
        SalesOrder so = get(soId);
        if (!so.isPickable()) {
            throw new BusinessException(so.getSoNumber() + " is " + so.getStatus() + " and can't be picked.");
        }
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("Pick quantity must be greater than zero.");
        }
        SalesOrderLine line = so.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Line not found on " + so.getSoNumber()));
        if (qty.compareTo(line.getQtyRemaining()) > 0) {
            throw new BusinessException("Only " + line.getQtyRemaining().stripTrailingZeros().toPlainString()
                    + " left to pick on line " + line.getLineNo() + ".");
        }
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found: " + locationId));
        Part part = line.getPart();

        BigDecimal onHand = inventoryService.totalOnHand(part);
        if (onHand.compareTo(qty) < 0) {
            throw new BusinessException("Not enough " + part.getPartNumber() + " on hand to pick "
                    + qty.stripTrailingZeros().toPlainString() + ".");
        }

        inventoryService.removeStock(part, location, qty, TransactionType.PICK, so.getSoNumber(),
                "Line " + line.getLineNo());
        line.setQtyPicked(line.getQtyPicked().add(qty));
        part.setQtyAllocated(part.getQtyAllocated().subtract(qty));

        boolean allPicked = so.getLines().stream().allMatch(SalesOrderLine::isFullyPicked);
        so.setStatus(allPicked ? SalesOrderStatus.PICKED : SalesOrderStatus.IN_PICKING);
        log.info("Picked {} of {} for {} from {}", qty, part.getPartNumber(), so.getSoNumber(), location.getCode());
    }

    public void ship(Long soId, String carrier, String trackingNumber) {
        SalesOrder so = get(soId);
        if (so.getStatus() != SalesOrderStatus.PICKED) {
            throw new BusinessException("Only fully picked orders can be shipped.");
        }
        if (carrier == null || carrier.isBlank()) {
            throw new BusinessException("Choose a carrier.");
        }
        so.setCarrier(carrier.trim());
        so.setTrackingNumber(trackingNumber == null ? null : trackingNumber.trim());
        so.setShippedAt(LocalDateTime.now());
        so.setStatus(SalesOrderStatus.SHIPPED);
        log.info("Shipped {} via {}", so.getSoNumber(), carrier);
    }

    public void cancel(Long soId) {
        SalesOrder so = get(soId);
        if (so.getStatus() == SalesOrderStatus.SHIPPED || so.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new BusinessException("This order is already closed.");
        }
        boolean anyPicked = so.getLines().stream().anyMatch(l -> l.getQtyPicked().signum() > 0);
        if (anyPicked) {
            throw new BusinessException("Can't cancel: items were already picked. Put them back with an adjustment first.");
        }
        so.setStatus(SalesOrderStatus.CANCELLED);
        log.info("Cancelled {}", so.getSoNumber());
    }

    /**
     * Used by the REST API: create, fill and issue an order in one step.
     * If anything fails, the whole order is rolled back.
     */
    public SalesOrder createFromApi(String customerCode, String externalRef, List<OrderLineRequest> lines) {
        Customer customer = customerRepository.findByCode(customerCode)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerCode));
        SalesOrder so = create(customer.getId(), OrderSource.API, externalRef);
        for (OrderLineRequest req : lines) {
            Part part = partRepository.findByPartNumber(req.partNumber())
                    .orElseThrow(() -> new NotFoundException("Part not found: " + req.partNumber()));
            addLine(so.getId(), part.getId(), req.quantity(), req.unitPrice());
        }
        issue(so.getId());
        return so;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(SalesOrder so) {
        return so.getLines().stream()
                .map(SalesOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
