package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.MaterialPrice;
import com.transport.erp.service.MaterialPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/material-prices")
@CrossOrigin(origins = "*")
public class MaterialPriceController {

    @Autowired
    private MaterialPriceService priceService;

    @GetMapping
    public ApiResponse<List<MaterialPrice>> getPrices(@RequestParam Long materialId) {
        List<MaterialPrice> prices = priceService.getPricesByMaterial(materialId);
        return ApiResponse.success(prices, "Material Prices fetched successfully");
    }

    @PostMapping
    public ApiResponse<MaterialPrice> create(
            @RequestParam Long materialId,
            @RequestBody MaterialPrice price) {
        MaterialPrice created = priceService.create(materialId, price);
        return ApiResponse.success(created, "Material Price configuration created successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        priceService.delete(id);
        return ApiResponse.success(null, "Material Price entry deleted successfully");
    }
}
