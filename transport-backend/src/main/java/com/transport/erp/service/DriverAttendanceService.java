package com.transport.erp.service;

import com.transport.erp.model.Driver;
import com.transport.erp.model.DriverAttendance;
import com.transport.erp.repository.DriverAttendanceRepository;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DriverAttendanceService {

    @Autowired
    private DriverAttendanceRepository attendanceRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    public List<DriverAttendance> getAttendanceByDriver(Long driverId) {
        parentAccess.requireDriver(driverId);
        return attendanceRepository.findByDriverIdAndIsDeletedFalse(driverId);
    }

    @Transactional
    public DriverAttendance logAttendance(Long driverId, DriverAttendance log) {
        Driver driver = parentAccess.requireDriver(driverId);

        log.setDriver(driver);
        log.setIsDeleted(false);
        log.setAttendanceDate(log.getAttendanceDate() != null ? log.getAttendanceDate() : LocalDate.now());
        log.setCode("ATT_" + driverId + "_" + log.getAttendanceDate());
        log.setName("Driver Attendance Log");
        log.setCompanyId(driver.getCompanyId());
        log.setBranchId(driver.getBranchId());

        return attendanceRepository.save(log);
    }
}
