package com.gamevault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Stores uploaded images on disk (outside the database) and exposes them under /uploads/**. */
@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png",  ".png",
            "image/gif",  ".gif",
            "image/webp", ".webp",
            "image/avif", ".avif"
    );

    private final Path uploadsDir;

    public FileStorageService(@Value("${app.uploads.dir:uploads}") String uploadsDir) {
        this.uploadsDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar a pasta de uploads: " + this.uploadsDir, e);
        }
    }

    public Path getUploadsDir() {
        return uploadsDir;
    }

    /** Saves an uploaded image and returns its public path, e.g. "/uploads/3f2a….png". */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ficheiro vazio.");
        }
        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Só é possível enviar imagens (JPEG, PNG, GIF, WebP ou AVIF).");
        }
        String filename = UUID.randomUUID() + extension;
        Path target = uploadsDir.resolve(filename).normalize();
        if (!target.startsWith(uploadsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de ficheiro inválido.");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao guardar o ficheiro.", e);
        }
        return "/uploads/" + filename;
    }

    /**
     * Deletes a previously uploaded file when its URL is replaced.
     * Ignores anything that isn't one of our /uploads/ files (external URLs, emojis, data URLs, null).
     */
    public void deleteIfUpload(String url) {
        if (url == null) return;
        int idx = url.indexOf("/uploads/");
        if (idx < 0) return;
        String filename = url.substring(idx + "/uploads/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) return;
        Path target = uploadsDir.resolve(filename).normalize();
        if (!target.startsWith(uploadsDir)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // best-effort cleanup — never fail the request because the old file couldn't be removed
        }
    }
}
