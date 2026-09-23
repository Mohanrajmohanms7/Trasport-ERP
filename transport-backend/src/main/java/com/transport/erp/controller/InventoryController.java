package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.InventoryTransactionResponse;
import com.transport.erp.dto.OpeningBalanceRequest;
import com.transport.erp.dto.WarehouseStockResponse;
import com.transport.erp.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/stock")
    public ApiResponse<Page<WarehouseStockResponse>> listStock(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sparePartId,
            @RequestParam(required = false) String code,
            Pageable pageable) {
        return ApiResponse.success(
                inventoryService.listStock(companyId, branchId, warehouseId, sparePartId, code, pageable),
                "Stock fetched successfully");
    }

    @GetMapping("/stock/{id}")
    public ApiResponse<WarehouseStockResponse> getStock(@PathVariable Long id) {
        return ApiResponse.success(inventoryService.getStock(id), "Stock fetched successfully");
    }

    @PostMapping("/stock/opening-balance")
    public ApiResponse<WarehouseStockResponse> openingBalance(
            @RequestBody OpeningBalanceRequest request,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.success(
                inventoryService.createOpeningBalance(request, username),
                "Opening balance created successfully");
    }

    @GetMapping("/transactions")
    public ApiResponse<Page<InventoryTransactionResponse>> listTransactions(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sparePartId,
            @RequestParam(required = false) String transactionType,
            Pageable pageable) {
        return ApiResponse.success(
                inventoryService.listTransactions(companyId, branchId, warehouseId, sparePartId, transactionType, pageable),
                "Inventory transactions fetched successfully");
    }

    @GetMapping("/transactions/{id}")
    public ApiResponse<InventoryTransactionResponse> getTransaction(@PathVariable Long id) {
        return ApiResponse.success(inventoryService.getTransaction(id), "Inventory transaction fetched successfully");
    }
}
