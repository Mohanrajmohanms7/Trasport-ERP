package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Attachment;
import com.transport.erp.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    @GetMapping
    public ApiResponse<List<Attachment>> list(@RequestParam String entityType, @RequestParam Long entityId) {
        return ApiResponse.success(attachmentService.list(entityType, entityId), "Attachments");
    }

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<Attachment> add(@RequestParam String entityType, @RequestParam Long entityId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String remarks,
                                       Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        return ApiResponse.success(attachmentService.add(entityType, entityId, file, category, remarks, username), "File attached");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id, Principal principal) {
        attachmentService.remove(id, principal != null ? principal.getName() : "system");
        return ApiResponse.success(null, "Attachment removed");
    }
}
