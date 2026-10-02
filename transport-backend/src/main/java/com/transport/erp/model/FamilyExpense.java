package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Family / personal expense of the owner (optional add-on, docs/FAMILY_EXPENSES.md).
 * Kept apart from the business on purpose: no vehicle / driver / trip, no journal entry, no effect on business figures.
 * {@code description} (from BaseEntity) holds what the money was spent on.
 */
@Getter
@Setter
@Entity
@Table(name = "family_expenses")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FamilyExpense extends BaseEntity {

    @Column(name = "expense_number", nullable = false, length = 50)
    private String expenseNumber;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /** Code of a FAMILY_EXPENSE_CATEGORY dropdown value (GROCERIES, EDUCATION …). */
    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Code of a PAYMENT_METHOD dropdown value (CASH, UPI, BANK_TRANSFER …). */
    @Column(name = "payment_mode", nullable = false, length = 50)
    private String paymentMode;

    @Column(name = "member_name", length = 100)
    private String memberName;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;
}
