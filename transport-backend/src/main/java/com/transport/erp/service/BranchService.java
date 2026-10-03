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

    @org.springframework.beans.factory.annotation.Autowired
    private PlanLimitService planLimits;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
        validate(branch);
        if (branchRepository.findByCompanyIdAndCodeAndIsDeletedFalse(branch.getCompanyId(), branch.getCode()).isPresent()) {
            throw new IllegalArgumentException("Branch code already exists in this company: " + branch.getCode());
        }
        branch.setIsDeleted(false);
        if (branch.getStatus() == null || "ACTIVE".equals(branch.getStatus())) {
            planLimits.assertCanAdd(branch.getCompanyId(), PlanLimitService.Kind.BRANCHES);
        }
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
        validate(branch);

        return branchRepository.save(branch);
    }

    @Transactional
    public void delete(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
        // A branch with people, vehicles or transactions is history: deactivate it instead.
        Integer used = jdbcTemplate.queryForObject(
                "SELECT (SELECT COUNT(*) FROM app_users WHERE branch_id = ? AND is_deleted = false)"
                        + " + (SELECT COUNT(*) FROM vehicles WHERE branch_id = ? AND is_deleted = false)"
                        + " + (SELECT COUNT(*) FROM drivers WHERE branch_id = ? AND is_deleted = false)"
                        + " + (SELECT COUNT(*) FROM trips WHERE branch_id = ? AND is_deleted = false)"
                        + " + (SELECT COUNT(*) FROM sales_invoices WHERE branch_id = ? AND is_deleted = false)"
                        + " + (SELECT COUNT(*) FROM warehouses WHERE branch_id = ? AND is_deleted = false)",
                Integer.class, id, id, id, id, id, id);
        if (used != null && used > 0) {
            throw new IllegalArgumentException(branch.getName() + " has users, vehicles, drivers, trips, invoices or warehouses. Set it Inactive instead of deleting.");
        }
        Integer others = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM branches WHERE company_id = ? AND id <> ? AND is_deleted = false", Integer.class, branch.getCompanyId(), id);
        if (others == null || others == 0) {
            throw new IllegalArgumentException("A company needs at least one branch.");
        }
        branch.setIsDeleted(true);
        branchRepository.save(branch);
    }

    @Transactional
    public Branch toggleStatus(Long id) {
        Branch branch = branchRepository.findById(id)
                .filter(b -> !b.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
        if (!"ACTIVE".equals(branch.getStatus())) {
            planLimits.assertCanAdd(branch.getCompanyId(), PlanLimitService.Kind.BRANCHES);
        }
        branch.setStatus("ACTIVE".equals(branch.getStatus()) ? "INACTIVE" : "ACTIVE");
        return branchRepository.save(branch);
    }

    /** Per-branch counts for Branch Master (people, fleet, stores). */
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> summary(Long companyId) {
        return jdbcTemplate.queryForList(
                "SELECT b.id AS branch_id,"
                        + " (SELECT COUNT(*) FROM app_users u WHERE u.branch_id = b.id AND u.is_deleted = false) AS users,"
                        + " (SELECT COUNT(*) FROM vehicles v WHERE v.branch_id = b.id AND v.is_deleted = false) AS vehicles,"
                        + " (SELECT COUNT(*) FROM drivers d WHERE d.branch_id = b.id AND d.is_deleted = false) AS drivers,"
                        + " (SELECT COUNT(*) FROM warehouses w WHERE w.branch_id = b.id AND w.is_deleted = false) AS warehouses"
                        + " FROM branches b WHERE b.company_id = ? AND b.is_deleted = false", companyId);
    }

    private static void validate(Branch b) {
        if (b.getCode() == null || b.getCode().isBlank()) throw new IllegalArgumentException("Branch code is required.");
        if (b.getName() == null || b.getName().isBlank()) throw new IllegalArgumentException("Branch name is required.");
        if (b.getGstNumber() != null && !b.getGstNumber().isBlank()) {
            String g = b.getGstNumber().trim().toUpperCase();
            if (!g.matches("\\d{2}[A-Z0-9]{13}")) {
                throw new IllegalArgumentException("Branch GSTIN must be 15 characters starting with the 2-digit state code.");
            }
            b.setGstNumber(g);
        }
    }
}
