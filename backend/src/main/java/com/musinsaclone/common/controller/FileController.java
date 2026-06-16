package com.musinsaclone.common.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.common.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "images") String directory) {
        String url = fileUploadService.upload(file, directory);
        return ApiResponse.ok(Map.of("url", url));
    }
}
