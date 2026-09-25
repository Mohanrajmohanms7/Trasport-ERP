package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

/** Photo for a vehicle, driver or spare part: POST /api/v1/photos/{vehicles|drivers|spare-parts}/{id}. */
@RestController
@RequestMapping("/api/v1/photos")
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @PostMapping(value = "/{type}/{id}", consumes = "multipart/form-data")
    public ApiResponse<Map<String, String>> upload(@PathVariable String type, @PathVariable Long id,
                                                   @RequestParam("file") MultipartFile file, Principal principal) {
        String name = photoService.setPhoto(type, id, file, principal != null ? principal.getName() : "system");
        return ApiResponse.success(Map.of("photoFile", name, "fileUrl", "/api/v1/files/download/" + name), "Photo saved");
    }

    @DeleteMapping("/{type}/{id}")
    public ApiResponse<Void> remove(@PathVariable String type, @PathVariable Long id, Principal principal) {
        photoService.removePhoto(type, id, principal != null ? principal.getName() : "system");
        return ApiResponse.success(null, "Photo removed");
    }
}
