package com.transport.erp.repository;

import com.transport.erp.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameAndIsDeletedFalse(String username);
    boolean existsByUsernameAndIsDeletedFalse(String username);
    Optional<AppUser> findByCodeAndIsDeletedFalse(String code);
    Optional<AppUser> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);

    @EntityGraph(attributePaths = {"roles"})
    Page<AppUser> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"roles"})
    Page<AppUser> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(Long companyId, String name, String code, Pageable pageable);

    Page<AppUser> findByIsDeletedFalse(Pageable pageable);
    Page<AppUser> findByIsDeletedFalseAndUsernameContainingIgnoreCaseOrIsDeletedFalseAndEmailContainingIgnoreCase(String username, String email, Pageable pageable);
}
