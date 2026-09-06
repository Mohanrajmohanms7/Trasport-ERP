package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.CustomerDeliverySite;
import com.transport.erp.service.CustomerDeliverySiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/delivery-sites")
@CrossOrigin(origins = "*")
public class CustomerDeliverySiteController {

    @Autowired
    private CustomerDeliverySiteService siteService;

    @GetMapping
    public ApiResponse<List<CustomerDeliverySite>> getSites(@PathVariable Long customerId) {
        List<CustomerDeliverySite> sites = siteService.getSitesByCustomer(customerId);
        return ApiResponse.success(sites, "Customer delivery sites fetched successfully");
    }

    @PostMapping
    public ApiResponse<CustomerDeliverySite> addSite(
            @PathVariable Long customerId,
            @RequestBody CustomerDeliverySite site) {
        CustomerDeliverySite created = siteService.addSite(customerId, site);
        return ApiResponse.success(created, "Customer delivery site created successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSite(
            @PathVariable Long customerId,
            @PathVariable Long id) {
        try {
            siteService.deleteSite(id);
            return ApiResponse.success(null, "Customer delivery site deleted successfully");
        } catch (com.transport.erp.exception.BusinessValidationException e) {
            return ApiResponse.error(e.getErrors(), e.getTitle() != null ? e.getTitle() : e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(java.util.Collections.singletonList(e.getMessage()), "Failed to delete delivery site");
        }
    }
}
