package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.CustomerLedger;
import com.transport.erp.service.CustomerLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer-ledger")
@CrossOrigin(origins = "*")
public class CustomerLedgerController {

    @Autowired
    private CustomerLedgerService ledgerService;

    @GetMapping("/{customerId}")
    public ApiResponse<List<CustomerLedger>> getLedger(@PathVariable Long customerId) {
        List<CustomerLedger> ledger = ledgerService.getLedgerByCustomer(customerId);
        return ApiResponse.success(ledger, "Customer ledger fetched successfully");
    }
}
