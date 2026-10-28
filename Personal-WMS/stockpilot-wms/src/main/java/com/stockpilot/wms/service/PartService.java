package com.stockpilot.wms.service;

import com.stockpilot.wms.domain.Part;
import com.stockpilot.wms.repository.InventoryLevelRepository;
import com.stockpilot.wms.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PartService {

    private final PartRepository partRepository;
    private final InventoryLevelRepository levelRepository;

    public PartService(PartRepository partRepository, InventoryLevelRepository levelRepository) {
        this.partRepository = partRepository;
        this.levelRepository = levelRepository;
    }

    @Transactional(readOnly = true)
    public List<Part> findAll() {
        return partRepository.findAllByOrderByPartNumberAsc();
    }

    @Transactional(readOnly = true)
    public List<PartSummary> search(String query) {
        List<Part> parts = (query == null || query.isBlank())
                ? partRepository.findAllByOrderByPartNumberAsc()
                : partRepository.findByPartNumberContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPartNumberAsc(
                        query.trim(), query.trim());
        return parts.stream().map(this::summarize).toList();
    }

    @Transactional(readOnly = true)
    public Part get(Long id) {
        return partRepository.findById(id).orElseThrow(() -> new NotFoundException("Part not found: " + id));
    }

    @Transactional(readOnly = true)
    public Part getByNumber(String partNumber) {
        return partRepository.findByPartNumber(partNumber)
                .orElseThrow(() -> new NotFoundException("Part not found: " + partNumber));
    }

    @Transactional(readOnly = true)
    public PartSummary summarize(Part part) {
        BigDecimal onHand = levelRepository.sumOnHand(part);
        return new PartSummary(part, onHand == null ? BigDecimal.ZERO : onHand);
    }

    public Part create(String partNumber, String description, BigDecimal standardCost, BigDecimal reorderPoint) {
        String normalized = partNumber.trim().toUpperCase();
        if (partRepository.existsByPartNumber(normalized)) {
            throw new BusinessException("Part number " + normalized + " already exists.");
        }
        Part part = new Part();
        part.setPartNumber(normalized);
        part.setDescription(description.trim());
        part.setStandardCost(standardCost == null ? BigDecimal.ZERO : standardCost);
        part.setReorderPoint(reorderPoint == null ? BigDecimal.ZERO : reorderPoint);
        return partRepository.save(part);
    }

    public Part update(Long id, String description, BigDecimal standardCost, BigDecimal reorderPoint, boolean active) {
        Part part = get(id);
        part.setDescription(description.trim());
        part.setStandardCost(standardCost);
        part.setReorderPoint(reorderPoint);
        part.setActive(active);
        return part;
    }
}
