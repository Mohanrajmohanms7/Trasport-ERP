package com.transport.erp.repository;

import com.transport.erp.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameAndIsDeletedFalse(String username);

    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM AppUser u WHERE u.id = :id")
    Optional<AppUser> findWithRolesById(@Param("id") Long id);
    boolean existsByUsernameAndIsDeletedFalse(String username);
    Optional<AppUser> findByCodeAndIsDeletedFalse(String code);
    Optional<AppUser> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);

    @EntityGraph(attributePaths = {"roles"})
    Page<AppUser> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"roles"})
    /** Parenthesised on purpose: the derived-name version OR-ed the code match outside the company filter. */
    @Query("SELECT e FROM AppUser e WHERE e.companyId = :companyId AND e.isDeleted = false AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :code, '%')))")
    Page<AppUser> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(@Param("companyId") Long companyId, @Param("name") String name, @Param("code") String code, Pageable pageable);

    Page<AppUser> findByIsDeletedFalse(Pageable pageable);
    Page<AppUser> findByIsDeletedFalseAndUsernameContainingIgnoreCaseOrIsDeletedFalseAndEmailContainingIgnoreCase(String username, String email, Pageable pageable);
}
