package com.transport.erp.service;

import com.transport.erp.model.LookupValue;
import com.transport.erp.repository.LookupValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class LookupValueService {

    @Autowired
    private LookupValueRepository lookupValueRepository;

    public Page<LookupValue> getAllByType(Long companyId, String type, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return lookupValueRepository.findByCompanyIdAndTypeAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, type, search, search, pageable);
        }
        return lookupValueRepository.findByCompanyIdAndTypeAndIsDeletedFalse(companyId, type, pageable);
    }

    public List<LookupValue> getListByType(Long companyId, String type) {
        return lookupValueRepository.findByCompanyIdAndTypeAndIsDeletedFalse(companyId, type);
    }

    public Optional<LookupValue> getById(Long id) {
        return lookupValueRepository.findById(id).filter(l -> !l.getIsDeleted());
    }

    @Transactional
    public LookupValue create(LookupValue lookupValue) {
        if (lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(
                lookupValue.getCompanyId(), lookupValue.getType(), lookupValue.getCode()).isPresent()) {
            throw new IllegalArgumentException(String.format("Lookup code '%s' already exists for type '%s'",
                    lookupValue.getCode(), lookupValue.getType()));
        }
        lookupValue.setIsDeleted(false);
        return lookupValueRepository.save(lookupValue);
    }

    @Transactional
    public LookupValue update(Long id, LookupValue lookupValueDetails) {
        LookupValue lookupValue = lookupValueRepository.findById(id)
                .filter(l -> !l.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Lookup value not found: " + id));

        Optional<LookupValue> existing = lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(
                lookupValueDetails.getCompanyId(), lookupValueDetails.getType(), lookupValueDetails.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException(String.format("Lookup code '%s' already exists for type '%s'",
                    lookupValueDetails.getCode(), lookupValueDetails.getType()));
        }

        lookupValue.setCode(lookupValueDetails.getCode());
        lookupValue.setName(lookupValueDetails.getName());
        lookupValue.setDescription(lookupValueDetails.getDescription());
        lookupValue.setStatus(lookupValueDetails.getStatus());
        lookupValue.setParent(lookupValueDetails.getParent());
        lookupValue.setCompanyId(lookupValueDetails.getCompanyId());
        lookupValue.setBranchId(lookupValueDetails.getBranchId());

        return lookupValueRepository.save(lookupValue);
    }

    @Transactional
    public void delete(Long id) {
        LookupValue lookupValue = lookupValueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lookup value not found: " + id));
        lookupValue.setIsDeleted(true);
        lookupValueRepository.save(lookupValue);
    }

    @Transactional
    public LookupValue toggleStatus(Long id) {
        LookupValue lookupValue = lookupValueRepository.findById(id)
                .filter(l -> !l.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Lookup value not found: " + id));
        lookupValue.setStatus("ACTIVE".equals(lookupValue.getStatus()) ? "INACTIVE" : "ACTIVE");
        return lookupValueRepository.save(lookupValue);
    }
}
