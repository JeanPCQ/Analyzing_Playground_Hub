package com.stockpilot.wms.service;

import com.stockpilot.wms.domain.*;
import com.stockpilot.wms.repository.InventoryLevelRepository;
import com.stockpilot.wms.repository.InventoryTransactionRepository;
import com.stockpilot.wms.repository.LocationRepository;
import com.stockpilot.wms.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * The only place that changes inventory_level. Every change also writes an
 * inventory_transaction row, so the history always explains the current quantity.
 */
@Service
@Transactional
public class InventoryService {

    private final InventoryLevelRepository levelRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final PartRepository partRepository;
    private final LocationRepository locationRepository;

    public InventoryService(InventoryLevelRepository levelRepository,
                            InventoryTransactionRepository transactionRepository,
                            PartRepository partRepository,
                            LocationRepository locationRepository) {
        this.levelRepository = levelRepository;
        this.transactionRepository = transactionRepository;
        this.partRepository = partRepository;
        this.locationRepository = locationRepository;
    }

    // ---- Queries ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<InventoryLevel> allLevels() {
        return levelRepository.findAllByOrderByPartPartNumberAscLocationCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<InventoryLevel> levelsFor(Part part) {
        return levelRepository.findByPartOrderByLocationCodeAsc(part);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalOnHand(Part part) {
        BigDecimal total = levelRepository.sumOnHand(part);
        return total == null ? BigDecimal.ZERO : total;
    }

    @Transactional(readOnly = true)
    public BigDecimal onHandAt(Part part, Location location) {
        return levelRepository.findByPartAndLocation(part, location)
                .map(InventoryLevel::getQtyOnHand)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> recentTransactions() {
        return transactionRepository.findTop200ByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> transactionsFor(Part part) {
        return transactionRepository.findByPartOrderByIdDesc(part);
    }

    // ---- Low-level movements (callers are responsible for business rules) --

    public void addStock(Part part, Location location, BigDecimal qty, TransactionType type,
                         String reference, String note) {
        InventoryLevel level = findOrCreateLevel(part, location);
        level.setQtyOnHand(level.getQtyOnHand().add(qty));
        levelRepository.save(level);
        log(part, location, qty, type, reference, note);
    }

    public void removeStock(Part part, Location location, BigDecimal qty, TransactionType type,
                            String reference, String note) {
        InventoryLevel level = findOrCreateLevel(part, location);
        level.setQtyOnHand(level.getQtyOnHand().subtract(qty));
        levelRepository.save(level);
        log(part, location, qty.negate(), type, reference, note);
    }

    // ---- User actions -----------------------------------------------------

    /** Manual correction, e.g. found extra stock or damaged goods. Positive or negative. */
    public void adjust(Long partId, Long locationId, BigDecimal qtyChange, String reason) {
        if (qtyChange == null || qtyChange.signum() == 0) {
            throw new BusinessException("Enter a quantity other than zero.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A reason is required for every adjustment.");
        }
        Part part = getPart(partId);
        Location location = getLocation(locationId);
        BigDecimal current = onHandAt(part, location);
        if (current.add(qtyChange).signum() < 0) {
            throw new BusinessException("Adjustment would make " + location.getCode() + " negative (on hand: "
                    + current.stripTrailingZeros().toPlainString() + ").");
        }
        if (qtyChange.signum() > 0) {
            addStock(part, location, qtyChange, TransactionType.ADJUST, "ADJ", reason.trim());
        } else {
            removeStock(part, location, qtyChange.negate(), TransactionType.ADJUST, "ADJ", reason.trim());
        }
    }

    /** Move stock from one location to another. */
    public void transfer(Long partId, Long fromLocationId, Long toLocationId, BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("Transfer quantity must be greater than zero.");
        }
        if (fromLocationId.equals(toLocationId)) {
            throw new BusinessException("Choose two different locations.");
        }
        Part part = getPart(partId);
        Location from = getLocation(fromLocationId);
        Location to = getLocation(toLocationId);
        BigDecimal available = onHandAt(part, from);
        if (available.compareTo(qty) < 0) {
            throw new BusinessException("Only " + available.stripTrailingZeros().toPlainString() + " of "
                    + part.getPartNumber() + " in " + from.getCode() + ".");
        }
        String ref = from.getCode() + " > " + to.getCode();
        removeStock(part, from, qty, TransactionType.TRANSFER_OUT, ref, null);
        addStock(part, to, qty, TransactionType.TRANSFER_IN, ref, null);
    }

    // ---- Helpers ----------------------------------------------------------

    private InventoryLevel findOrCreateLevel(Part part, Location location) {
        return levelRepository.findByPartAndLocation(part, location)
                .orElseGet(() -> new InventoryLevel(part, location));
    }

    private void log(Part part, Location location, BigDecimal qtyChange, TransactionType type,
                     String reference, String note) {
        transactionRepository.save(new InventoryTransaction(part, location, qtyChange, type, reference, note,
                CurrentUser.username()));
    }

    private Part getPart(Long id) {
        return partRepository.findById(id).orElseThrow(() -> new NotFoundException("Part not found: " + id));
    }

    private Location getLocation(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new NotFoundException("Location not found: " + id));
    }
}
