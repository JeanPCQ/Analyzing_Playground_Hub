package com.stockpilot.wms.service;

import com.stockpilot.wms.domain.Customer;
import com.stockpilot.wms.domain.Location;
import com.stockpilot.wms.domain.LocationType;
import com.stockpilot.wms.domain.Vendor;
import com.stockpilot.wms.repository.CustomerRepository;
import com.stockpilot.wms.repository.LocationRepository;
import com.stockpilot.wms.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Locations, vendors and customers: the simple lookup records the rest of the system points at. */
@Service
@Transactional
public class MasterDataService {

    private final LocationRepository locationRepository;
    private final VendorRepository vendorRepository;
    private final CustomerRepository customerRepository;

    public MasterDataService(LocationRepository locationRepository, VendorRepository vendorRepository,
                             CustomerRepository customerRepository) {
        this.locationRepository = locationRepository;
        this.vendorRepository = vendorRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Location> allLocations() {
        return locationRepository.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<Location> activeLocations() {
        return locationRepository.findByActiveTrueOrderByCodeAsc();
    }

    public Location createLocation(String code, String name, String zone, LocationType type) {
        String normalized = required(code, "Code").toUpperCase();
        if (locationRepository.findByCode(normalized).isPresent()) {
            throw new BusinessException("Location " + normalized + " already exists.");
        }
        Location location = new Location();
        location.setCode(normalized);
        location.setName(required(name, "Name"));
        location.setZone(required(zone, "Zone").toUpperCase());
        location.setType(type);
        return locationRepository.save(location);
    }

    public void toggleLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found: " + id));
        location.setActive(!location.isActive());
    }

    @Transactional(readOnly = true)
    public List<Vendor> allVendors() {
        return vendorRepository.findAllByOrderByNameAsc();
    }

    public Vendor createVendor(String code, String name, String email, String phone) {
        String normalized = required(code, "Code").toUpperCase();
        if (vendorRepository.existsByCode(normalized)) {
            throw new BusinessException("Vendor " + normalized + " already exists.");
        }
        Vendor vendor = new Vendor();
        vendor.setCode(normalized);
        vendor.setName(required(name, "Name"));
        vendor.setEmail(blankToNull(email));
        vendor.setPhone(blankToNull(phone));
        return vendorRepository.save(vendor);
    }

    @Transactional(readOnly = true)
    public List<Customer> allCustomers() {
        return customerRepository.findAllByOrderByNameAsc();
    }

    public Customer createCustomer(String code, String name, String email, String shipTo) {
        String normalized = required(code, "Code").toUpperCase();
        if (customerRepository.existsByCode(normalized)) {
            throw new BusinessException("Customer " + normalized + " already exists.");
        }
        Customer customer = new Customer();
        customer.setCode(normalized);
        customer.setName(required(name, "Name"));
        customer.setEmail(blankToNull(email));
        customer.setShipTo(blankToNull(shipTo));
        return customerRepository.save(customer);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + " is required.");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
