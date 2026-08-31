package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.FuelEntry;
import com.transport.erp.model.AppSetting;
import com.transport.erp.repository.FuelEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class FuelEntryService {

    @Autowired
    private FuelEntryRepository fuelEntryRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private AuditService auditService;




    @Autowired
    private AppSettingService settingService;




    public Page<FuelEntry> getFuelEntries(Long companyId, Pageable pageable) {
        return fuelEntryRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public FuelEntry getFuelEntryById(Long id) {
        FuelEntry entry = fuelEntryRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Fuel Entry not found: " + id));
        tenantAccess.assertOwned(entry.getCompanyId());
        return entry;
    }

    @Transactional
    public FuelEntry createFuelEntry(FuelEntry entry, String username) {
        String prefix = settingService.getByKey("PREFIX_FUEL").map(s -> s.getValueData()).orElse("FUEL-");
        entry.setFuelEntryNumber(prefix + System.currentTimeMillis());
        entry.setFuelDate(LocalDate.now());
        entry.setIsDeleted(false);
        entry.setCreatedBy(username);
        entry.setUpdatedBy(username);

        entry.setCompanyId(tenantAccess.resolveCompanyId(entry.getCompanyId()));
        entry.setBranchId(tenantAccess.resolveBranchId(entry.getBranchId()));


        // Calc totalAmount
        entry.setTotalAmount(entry.getFuelQuantity().multiply(entry.getRatePerLitre()));

        FuelEntry saved = fuelEntryRepository.save(entry);

        auditService.log(username, "FUEL_ENTRY_RECORDED", "fuel_entries", saved.getId(), null,
                "Recorded fuel entry number: " + saved.getFuelEntryNumber());

        return saved;
    }

    @Transactional
    public FuelEntry updateFuelEntry(Long id, FuelEntry details, String username) {
        FuelEntry existing = getFuelEntryById(id);

        existing.setFuelStation(details.getFuelStation());
        existing.setFuelQuantity(details.getFuelQuantity());
        existing.setRatePerLitre(details.getRatePerLitre());
        existing.setTotalAmount(details.getFuelQuantity().multiply(details.getRatePerLitre()));
        existing.setPaymentMethod(details.getPaymentMethod());
        existing.setInvoiceNumber(details.getInvoiceNumber());
        existing.setCurrentOdometer(details.getCurrentOdometer());
        existing.setPreviousOdometer(details.getPreviousOdometer());
        existing.setRemarks(details.getRemarks());
        existing.setUpdatedBy(username);

        FuelEntry saved = fuelEntryRepository.save(existing);

        auditService.log(username, "FUEL_ENTRY_UPDATED", "fuel_entries", saved.getId(), null,
                "Updated details for fuel entry: " + saved.getFuelEntryNumber());

        return saved;
    }

    @Transactional
    public void deleteFuelEntry(Long id, String username) {
        FuelEntry entry = getFuelEntryById(id);
        entry.setIsDeleted(true);
        entry.setUpdatedBy(username);
        fuelEntryRepository.save(entry);

        auditService.log(username, "FUEL_ENTRY_DELETED", "fuel_entries", entry.getId(), null,
                "Soft deleted fuel entry: " + entry.getFuelEntryNumber());
    }
}
