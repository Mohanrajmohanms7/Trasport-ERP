package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.TripDocument;
import com.transport.erp.service.TripDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/documents")
@CrossOrigin(origins = "*")
public class TripDocumentController {

    @Autowired
    private TripDocumentService docService;

    @GetMapping
    public ApiResponse<List<TripDocument>> getDocuments(@PathVariable Long tripId) {
        List<TripDocument> docs = docService.getDocumentsByTrip(tripId);
        return ApiResponse.success(docs, "Trip documents fetched successfully");
    }

    @PostMapping
    public ApiResponse<TripDocument> addDocument(
            @PathVariable Long tripId,
            @RequestBody TripDocument doc) {
        TripDocument created = docService.addDocument(tripId, doc);
        return ApiResponse.success(created, "Trip document uploaded successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable Long tripId,
            @PathVariable Long id) {
        docService.deleteDocument(id);
        return ApiResponse.success(null, "Trip document deleted successfully");
    }
}
