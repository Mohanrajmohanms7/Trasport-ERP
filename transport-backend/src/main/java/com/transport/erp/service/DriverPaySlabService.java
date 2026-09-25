package com.transport.erp.service;

import com.transport.erp.model.DriverPaySlab;
import com.transport.erp.repository.DriverPaySlabRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Company daily pay slabs. Saving replaces the whole set so it is always consistent. */
@Service
public class DriverPaySlabService {

    @Autowired private DriverPaySlabRepository slabRepository;
    @Autowired private TenantAccessService tenantAccess;
    @Autowired private AuditService auditService;

    public List<DriverPaySlab> getSlabs(Long companyId) {
        return slabRepository.findByCompanyIdAndIsDeletedFalseOrderByTripsFromAsc(tenantAccess.resolveCompanyId(companyId));
    }

    /** Existing payrolls keep the amounts they were calculated with; only new calculations use the new slabs. */
    @Transactional
    public List<DriverPaySlab> replaceSlabs(Long companyId, List<DriverPaySlab> requested, String username) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        List<DriverPaySlab> incoming = new ArrayList<>();
        for (DriverPaySlab in : requested == null ? List.<DriverPaySlab>of() : requested) {
            DriverPaySlab s = new DriverPaySlab();
            s.setTripsFrom(in.getTripsFrom());
            s.setTripsTo(in.getTripsTo());
            s.setDailyAmount(in.getDailyAmount());
            incoming.add(s);
        }
        List<DriverPaySlab> valid = DriverPayrollCalculator.validateSlabs(incoming);

        for (DriverPaySlab old : slabRepository.findByCompanyIdAndIsDeletedFalseOrderByTripsFromAsc(cid)) {
            old.setIsDeleted(true);
            old.setUpdatedBy(username);
            slabRepository.save(old);
        }
        List<DriverPaySlab> saved = new ArrayList<>();
        for (DriverPaySlab s : valid) {
            String range = s.getTripsTo() == null ? s.getTripsFrom() + "+" : s.getTripsFrom() + "-" + s.getTripsTo();
            s.setCompanyId(cid);
            s.setCode("SLAB-" + range);
            s.setName("Daily pay " + range + " trips");
            s.setStatus("ACTIVE");
            s.setIsDeleted(false);
            s.setCreatedBy(username);
            s.setUpdatedBy(username);
            saved.add(slabRepository.save(s));
        }
        auditService.log(username, "DRIVER_PAY_SLABS_UPDATED", "driver_pay_slabs", null, null,
                "Daily pay slabs replaced: " + saved.size() + " slab(s)");
        return saved;
    }
}
