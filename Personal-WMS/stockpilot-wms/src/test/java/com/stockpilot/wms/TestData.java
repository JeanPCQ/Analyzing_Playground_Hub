package com.stockpilot.wms;

import com.stockpilot.wms.domain.*;
import com.stockpilot.wms.repository.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/** Builds the records a test needs. Codes get a counter suffix so tests never collide. */
@Component
public class TestData {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final UomRepository uomRepository;
    private final LocationRepository locationRepository;
    private final PartRepository partRepository;
    private final VendorRepository vendorRepository;
    private final CustomerRepository customerRepository;

    public TestData(UomRepository uomRepository, LocationRepository locationRepository, PartRepository partRepository,
                    VendorRepository vendorRepository, CustomerRepository customerRepository) {
        this.uomRepository = uomRepository;
        this.locationRepository = locationRepository;
        this.partRepository = partRepository;
        this.vendorRepository = vendorRepository;
        this.customerRepository = customerRepository;
    }

    public Uom uom(String code, String factor) {
        return uomRepository.findByCode(code).orElseGet(() -> {
            Uom uom = new Uom();
            uom.setCode(code);
            uom.setName(code);
            uom.setFactorToEach(new BigDecimal(factor));
            return uomRepository.save(uom);
        });
    }

    public Uom each() {
        return uom("EA", "1");
    }

    public Uom caseOf12() {
        return uom("CS", "12");
    }

    public Location location(String prefix) {
        Location location = new Location();
        location.setCode(prefix + "-" + SEQ.incrementAndGet());
        location.setName("Test " + prefix);
        location.setZone("T");
        location.setType(LocationType.STORAGE);
        return locationRepository.save(location);
    }

    public Part part(String prefix) {
        Part part = new Part();
        part.setPartNumber(prefix + "-" + SEQ.incrementAndGet());
        part.setDescription("Test part " + prefix);
        part.setStandardCost(new BigDecimal("1.00"));
        return partRepository.save(part);
    }

    public Vendor vendor() {
        Vendor vendor = new Vendor();
        vendor.setCode("V" + SEQ.incrementAndGet());
        vendor.setName("Test vendor");
        return vendorRepository.save(vendor);
    }

    public Customer customer() {
        Customer customer = new Customer();
        customer.setCode("C" + SEQ.incrementAndGet());
        customer.setName("Test customer");
        return customerRepository.save(customer);
    }
}
