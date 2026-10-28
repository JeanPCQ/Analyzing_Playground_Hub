package com.stockpilot.wms.service;

import com.stockpilot.wms.domain.*;
import com.stockpilot.wms.repository.LocationRepository;
import com.stockpilot.wms.repository.PartRepository;
import com.stockpilot.wms.repository.PurchaseOrderRepository;
import com.stockpilot.wms.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Service
@Transactional
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);

    private final PurchaseOrderRepository poRepository;
    private final VendorRepository vendorRepository;
    private final PartRepository partRepository;
    private final LocationRepository locationRepository;
    private final UomService uomService;
    private final InventoryService inventoryService;

    public PurchaseOrderService(PurchaseOrderRepository poRepository, VendorRepository vendorRepository,
                                PartRepository partRepository, LocationRepository locationRepository,
                                UomService uomService, InventoryService inventoryService) {
        this.poRepository = poRepository;
        this.vendorRepository = vendorRepository;
        this.partRepository = partRepository;
        this.locationRepository = locationRepository;
        this.uomService = uomService;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAll() {
        return poRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> openForReceiving() {
        return poRepository.findByStatusInOrderByIdAsc(
                EnumSet.of(PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.PARTIALLY_RECEIVED));
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return poRepository.countByStatusIn(
                EnumSet.of(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.PARTIALLY_RECEIVED));
    }

    @Transactional(readOnly = true)
    public PurchaseOrder get(Long id) {
        return poRepository.findById(id).orElseThrow(() -> new NotFoundException("Purchase order not found: " + id));
    }

    public PurchaseOrder create(Long vendorId, LocalDate expectedDate, String notes) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + vendorId));
        PurchaseOrder po = new PurchaseOrder();
        po.setVendor(vendor);
        po.setExpectedDate(expectedDate);
        po.setNotes(notes);
        po = poRepository.save(po);
        po.setPoNumber(String.format("PO-%05d", po.getId()));
        log.info("Created {} for vendor {}", po.getPoNumber(), vendor.getCode());
        return po;
    }

    public void addLine(Long poId, Long partId, Long uomId, BigDecimal qty, BigDecimal unitCost) {
        PurchaseOrder po = get(poId);
        if (!po.isEditable()) {
            throw new BusinessException("Lines can only be added while the PO is a draft.");
        }
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("Quantity must be greater than zero.");
        }
        if (unitCost == null || unitCost.signum() < 0) {
            throw new BusinessException("Unit cost can't be negative.");
        }
        Part part = partRepository.findById(partId).orElseThrow(() -> new NotFoundException("Part not found: " + partId));
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setPurchaseOrder(po);
        line.setLineNo(po.getLines().size() + 1);
        line.setPart(part);
        line.setUom(uomService.get(uomId));
        line.setQtyOrdered(qty);
        line.setUnitCost(unitCost);
        po.getLines().add(line);
        poRepository.flush(); // write the new line now so it gets its ID
    }

    public void removeLine(Long poId, Long lineId) {
        PurchaseOrder po = get(poId);
        if (!po.isEditable()) {
            throw new BusinessException("Lines can only be removed while the PO is a draft.");
        }
        po.getLines().removeIf(l -> l.getId().equals(lineId));
        int n = 1;
        for (PurchaseOrderLine l : po.getLines()) {
            l.setLineNo(n++);
        }
    }

    public void issue(Long poId) {
        PurchaseOrder po = get(poId);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException("Only draft purchase orders can be issued.");
        }
        if (po.getLines().isEmpty()) {
            throw new BusinessException("Add at least one line before issuing.");
        }
        po.setStatus(PurchaseOrderStatus.ISSUED);
        log.info("Issued {}", po.getPoNumber());
    }

    public void cancel(Long poId) {
        PurchaseOrder po = get(poId);
        boolean anyReceived = po.getLines().stream().anyMatch(l -> l.getQtyReceived().signum() > 0);
        if (anyReceived) {
            throw new BusinessException("Can't cancel: some items were already received.");
        }
        if (po.getStatus() == PurchaseOrderStatus.CANCELLED || po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new BusinessException("This purchase order is already closed.");
        }
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        log.info("Cancelled {}", po.getPoNumber());
    }

    /**
     * Receive goods against one PO line into a location.
     * qty is in the line's unit of measure; inventory is stored in EA.
     */
    public void receive(Long poId, Long lineId, BigDecimal qty, Long locationId) {
        PurchaseOrder po = get(poId);
        if (!po.isReceivable()) {
            throw new BusinessException(po.getPoNumber() + " is " + po.getStatus() + " and can't be received.");
        }
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("Receive quantity must be greater than zero.");
        }
        PurchaseOrderLine line = po.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Line not found on " + po.getPoNumber()));
        if (qty.compareTo(line.getQtyRemaining()) > 0) {
            throw new BusinessException("Only " + line.getQtyRemaining().stripTrailingZeros().toPlainString() + " "
                    + line.getUom().getCode() + " left to receive on line " + line.getLineNo() + ".");
        }
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found: " + locationId));

        line.setQtyReceived(line.getQtyReceived().add(qty));
        if (qty.compareTo(line.getQtyOrdered()) >= 0) {
            line.setStatus(PoLineStatus.RECEIVED);
        } else {
            line.setStatus(PoLineStatus.PARTIAL);
        }

        BigDecimal eaches = uomService.toEach(qty, line.getUom());
        inventoryService.addStock(line.getPart(), location, eaches, TransactionType.RECEIVE,
                po.getPoNumber(), "Line " + line.getLineNo());

        updateStatus(po);
        log.info("Received {} {} of {} on {} into {}", qty, line.getUom().getCode(),
                line.getPart().getPartNumber(), po.getPoNumber(), location.getCode());
    }

    /** Total value of the order. */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(PurchaseOrder po) {
        double total = 0;
        for (PurchaseOrderLine line : po.getLines()) {
            total += line.getQtyOrdered().doubleValue() * line.getUnitCost().doubleValue();
        }
        return BigDecimal.valueOf(total);
    }

    private void updateStatus(PurchaseOrder po) {
        boolean allReceived = po.getLines().stream().allMatch(l -> l.getStatus() == PoLineStatus.RECEIVED);
        po.setStatus(allReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
    }
}
