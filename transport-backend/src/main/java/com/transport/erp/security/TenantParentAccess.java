package com.transport.erp.security;

import com.transport.erp.model.Customer;
import com.transport.erp.model.Driver;
import com.transport.erp.model.Vehicle;
import com.transport.erp.repository.CustomerRepository;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures nested customer/driver/vehicle child resources belong to the caller's tenant.
 */
@Component
public class TenantParentAccess {

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public Customer requireCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        tenantAccess.assertCompanyAccess(customer.getCompanyId());
        return customer;
    }

    @Transactional(readOnly = true)
    public Driver requireDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));
        tenantAccess.assertCompanyAccess(driver.getCompanyId());
        return driver;
    }

    @Transactional(readOnly = true)
    public Vehicle requireVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
        tenantAccess.assertCompanyAccess(vehicle.getCompanyId());
        return vehicle;
    }
}
