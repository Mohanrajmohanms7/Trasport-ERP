package com.transport.erp.service;

import com.transport.erp.model.Trip;
import com.transport.erp.model.TripDocument;
import com.transport.erp.repository.TripDocumentRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripDocumentService {

    @Autowired
    private TripDocumentRepository docRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<TripDocument> getDocumentsByTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        tenantAccess.assertCompanyAccess(trip.getCompanyId());
        return docRepository.findByTripIdAndIsDeletedFalse(tripId);
    }

    @Transactional
    public TripDocument addDocument(Long tripId, TripDocument doc) {
        Trip trip = tripRepository.findById(tripId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        tenantAccess.assertCompanyAccess(trip.getCompanyId());

        doc.setTrip(trip);
        doc.setIsDeleted(false);
        doc.setCode("TRIP_DOC_" + tripId + "_" + System.currentTimeMillis());
        doc.setName(doc.getDocType() != null ? doc.getDocType() : "Trip Document");
        doc.setCompanyId(trip.getCompanyId());
        doc.setBranchId(trip.getBranchId());

        return docRepository.save(doc);
    }

    @Transactional
    public void deleteDocument(Long id) {
        TripDocument doc = docRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Trip document not found: " + id));
        tenantAccess.assertCompanyAccess(doc.getCompanyId());
        doc.setIsDeleted(true);
        docRepository.save(doc);
    }
}
