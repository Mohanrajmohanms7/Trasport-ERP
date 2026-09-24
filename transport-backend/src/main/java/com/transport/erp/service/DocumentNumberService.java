package com.transport.erp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Issues gap-free, sequential document numbers per company, document type and
 * Indian financial year (April-March), e.g. {@code INV-2627/00001}.
 *
 * The counter row is incremented with a single UPDATE ... RETURNING, so the row stays
 * locked until the caller's transaction commits. Two concurrent saves therefore
 * serialize instead of producing duplicate numbers, and a rolled-back save
 * also rolls back its number (no gaps from failed saves).
 */
@Service
public class DocumentNumberService {

    public static final String INVOICE = "INVOICE";
    public static final String RECEIPT = "RECEIPT";
    public static final String BOOKING = "BOOKING";
    public static final String TRIP = "TRIP";
    public static final String EXPENSE = "EXPENSE";
    public static final String FUEL_ENTRY = "FUEL_ENTRY";
    public static final String FUEL_REQUEST = "FUEL_REQUEST";
    public static final String JOURNAL_VOUCHER = "JOURNAL_VOUCHER";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRED)
    public String next(Long companyId, String docType, String prefix, LocalDate documentDate) {
        long cid = companyId != null ? companyId : 0L;
        String fy = financialYearLabel(documentDate != null ? documentDate : LocalDate.now());

        jdbcTemplate.update(
                "INSERT INTO document_sequences (code, name, status, company_id, doc_type, fy_label, next_value, "
                        + "created_by, updated_by, created_date, updated_date, is_deleted, version) "
                        + "VALUES (?, ?, 'ACTIVE', ?, ?, ?, 1, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, 0) "
                        + "ON CONFLICT (company_id, doc_type, fy_label) DO NOTHING",
                docType + "-" + fy, docType + " sequence " + fy, cid, docType, fy);

        Long value = jdbcTemplate.queryForObject(
                "UPDATE document_sequences SET next_value = next_value + 1, updated_date = CURRENT_TIMESTAMP, version = version + 1 "
                        + "WHERE company_id = ? AND doc_type = ? AND fy_label = ? RETURNING next_value - 1",
                Long.class, cid, docType, fy);

        return format(prefix, fy, value != null ? value : 1L);
    }

    /** 2026-04-01 .. 2027-03-31 -> "2627". */
    public static String financialYearLabel(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return String.format("%02d%02d", startYear % 100, (startYear + 1) % 100);
    }

    public static String format(String prefix, String fyLabel, long value) {
        String p = prefix == null ? "" : prefix.trim();
        return p + fyLabel + "/" + String.format("%05d", value);
    }
}
