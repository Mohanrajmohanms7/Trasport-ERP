package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.AiPrediction;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.AiPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
public class AiPredictionController {

    @Autowired
    private AiPredictionService predictionService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping("/dashboard")
    public ApiResponse<Page<AiPrediction>> getAiDashboard(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<AiPrediction> data = predictionService.getPredictions(targetCompanyId, pageable);
        return ApiResponse.success(data, "AI prediction insights fetched successfully");
    }
}
