package com.transport.erp.service;

import com.transport.erp.model.Branch;
import com.transport.erp.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class BranchService {

    @Autowired
    private BranchRepository branchRepository;

    public Page<Branch> getAll(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return branchRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, search, search, pageable);
        }
        return branchRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Optional<Branch> getById(Long id) {
        return branchRepository.findById(id).filter(b -> !b.getIsDeleted());
    }

    @Transactional
    public Branch create(Branch branch) {
        if (branchRepository.findByCompanyIdAndCodeAndIsDeletedFalse(branch.getCompanyId(), branch.getCode()).isPresent()) {
            throw new IllegalArgumentException("Branch code already exists in this company: " + branch.getCode());
        }
        branch.setIsDeleted(false);
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch update(Long id, Branch branchDetails) {
        Branch branch = branchRepository.findById(id)
                .filter(b -> !b.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));

        Optional<Branch> existing = branchRepository.findByCompanyIdAndCodeAndIsDeletedFalse(branchDetails.getCompanyId(), branchDetails.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Branch code already exists in this company: " + branchDetails.getCode());
        }

        branch.setCode(branchDetails.getCode());
        branch.setName(branchDetails.getName());
        branch.setDescription(branchDetails.getDescription());
        branch.setStatus(branchDetails.getStatus());
        branch.setGstNumber(branchDetails.getGstNumber());
        branch.setManager(branchDetails.getManager());
        branch.setPhone(branchDetails.getPhone());
        branch.setEmail(branchDetails.getEmail());
        branch.setAddress(branchDetails.getAddress());
        branch.setLatitude(branchDetails.getLatitude());
        branch.setLongitude(branchDetails.getLongitude());

        return branchRepository.save(branch);
    }

    @Transactional
    public void delete(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
        branch.setIsDeleted(true);
        branchRepository.save(branch);
    }

    @Transactional
    public Branch toggleStatus(Long id) {
        Branch branch = branchRepository.findById(id)
                .filter(b -> !b.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
        branch.setStatus("ACTIVE".equals(branch.getStatus()) ? "INACTIVE" : "ACTIVE");
        return branchRepository.save(branch);
    }
}
