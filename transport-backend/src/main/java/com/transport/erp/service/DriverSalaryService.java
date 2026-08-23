package com.transport.erp.service;

import com.transport.erp.model.Driver;
import com.transport.erp.model.DriverSalary;
import com.transport.erp.repository.DriverSalaryRepository;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DriverSalaryService {

    @Autowired
    private DriverSalaryRepository salaryRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    public Optional<DriverSalary> getSalaryByDriver(Long driverId) {
        parentAccess.requireDriver(driverId);
        return salaryRepository.findByDriverIdAndIsDeletedFalse(driverId);
    }

    @Transactional
    public DriverSalary saveSalary(Long driverId, DriverSalary salary) {
        Driver driver = parentAccess.requireDriver(driverId);

        Optional<DriverSalary> existing = salaryRepository.findByDriverIdAndIsDeletedFalse(driverId);
        DriverSalary target;

        if (existing.isPresent()) {
            target = existing.get();
            target.setBasicSalary(salary.getBasicSalary());
            target.setOvertimeRate(salary.getOvertimeRate());
            target.setAdvanceTaken(salary.getAdvanceTaken());
        } else {
            target = salary;
            target.setDriver(driver);
            target.setIsDeleted(false);
            target.setCode("SAL_" + driverId);
            target.setName("Salary Configuration");
            target.setCompanyId(driver.getCompanyId());
            target.setBranchId(driver.getBranchId());
        }

        return salaryRepository.save(target);
    }
}
