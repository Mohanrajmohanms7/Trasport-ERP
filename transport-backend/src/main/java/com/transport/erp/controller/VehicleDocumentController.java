package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.VehicleDocument;
import com.transport.erp.service.VehicleDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/documents")
@CrossOrigin(origins = "*")
public class VehicleDocumentController {

    @Autowired
    private VehicleDocumentService docService;

    @GetMapping
    public ApiResponse<List<VehicleDocument>> getDocuments(@PathVariable Long vehicleId) {
        List<VehicleDocument> docs = docService.getDocumentsByVehicle(vehicleId);
        return ApiResponse.success(docs, "Vehicle documents fetched successfully");
    }

    @PostMapping
    public ApiResponse<VehicleDocument> addDocument(
            @PathVariable Long vehicleId,
            @RequestBody VehicleDocument doc) {
        VehicleDocument created = docService.addDocument(vehicleId, doc);
        return ApiResponse.success(created, "Vehicle document uploaded successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable Long vehicleId,
            @PathVariable Long id) {
        docService.deleteDocument(id);
        return ApiResponse.success(null, "Vehicle document deleted successfully");
    }
}
