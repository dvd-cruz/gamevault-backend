package com.gamevault.controller;

import com.gamevault.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService storage;

    public UploadController(FileStorageService storage) {
        this.storage = storage;
    }

    /** Uploads an image; returns its absolute URL, ready to store in iconUrl/avatar fields. */
    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        String path = storage.storeImage(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().path(path).toUriString();
        return Map.of("url", url);
    }
}
