package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.AiPrediction;
import com.transport.erp.repository.AiPredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiPredictionService {

    @Autowired
    private AiPredictionRepository predictionRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private AuditService auditService;




    public Page<AiPrediction> getPredictions(Long companyId, Pageable pageable) {
        return predictionRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    @Transactional
    public AiPrediction createPrediction(AiPrediction prediction, String username) {
        prediction.setIsDeleted(false);
        prediction.setCreatedBy(username);
        prediction.setUpdatedBy(username);

        prediction.setCompanyId(tenantAccess.resolveCompanyId(prediction.getCompanyId()));
        if (prediction.getBranchId() == null) prediction.setBranchId(1L);
        if (prediction.getCode() == null) prediction.setCode("AI-" + System.currentTimeMillis());
        if (prediction.getName() == null) prediction.setName("AI Telemetry Prediction");

        AiPrediction saved = predictionRepository.save(prediction);

        auditService.log(username, "AI_PREDICTION_GENERATED", "ai_predictions", saved.getId(), null,
                "Generated new AI prediction type: " + saved.getTargetType());

        return saved;
    }
}
