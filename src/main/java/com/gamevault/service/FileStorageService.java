package com.gamevault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded images either on Cloudflare R2 (when configured) or on local disk (dev fallback).
 * R2 mode returns absolute public URLs; disk mode returns "/uploads/**" paths served by the app.
 */
@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png",  ".png",
            "image/gif",  ".gif",
            "image/webp", ".webp",
            "image/avif", ".avif"
    );

    // ── R2 mode ──
    private final boolean useR2;
    private final S3Client s3;
    private final String bucket;
    private final String publicUrl;   // sem barra final

    // ── Disk mode (dev) ──
    private final Path uploadsDir;

    public FileStorageService(
            @Value("${app.uploads.dir:uploads}")          String uploadsDir,
            @Value("${app.storage.r2.endpoint:}")         String r2Endpoint,
            @Value("${app.storage.r2.access-key:}")       String r2AccessKey,
            @Value("${app.storage.r2.secret-key:}")       String r2SecretKey,
            @Value("${app.storage.r2.bucket:}")           String r2Bucket,
            @Value("${app.storage.r2.public-url:}")       String r2PublicUrl) {

        this.useR2 = !r2Endpoint.isBlank() && !r2AccessKey.isBlank()
                  && !r2SecretKey.isBlank() && !r2Bucket.isBlank() && !r2PublicUrl.isBlank();

        if (useR2) {
            this.bucket = r2Bucket;
            this.publicUrl = r2PublicUrl.replaceAll("/+$", "");
            this.s3 = S3Client.builder()
                    .endpointOverride(URI.create(r2Endpoint))
                    .region(Region.of("auto"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(r2AccessKey, r2SecretKey)))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
            this.uploadsDir = null;
        } else {
            this.s3 = null;
            this.bucket = null;
            this.publicUrl = null;
            this.uploadsDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
            try {
                Files.createDirectories(this.uploadsDir);
            } catch (IOException e) {
                throw new IllegalStateException("Não foi possível criar a pasta de uploads: " + this.uploadsDir, e);
            }
        }
    }

    /** Null in R2 mode; used by CorsConfig to serve local files in disk mode. */
    public Path getUploadsDir() {
        return uploadsDir;
    }

    /** Saves an uploaded image and returns its public URL (R2 absolute URL, or "/uploads/…" on disk). */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ficheiro vazio.");
        }
        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Só é possível enviar imagens (JPEG, PNG, GIF, WebP ou AVIF).");
        }
        String filename = UUID.randomUUID() + extension;

        if (useR2) {
            try {
                s3.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(filename)
                                .contentType(file.getContentType())
                                .build(),
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao guardar o ficheiro.", e);
            }
            return publicUrl + "/" + filename;
        }

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
     * Ignores anything that isn't one of our own uploads (external URLs, emojis, data URLs, null).
     */
    public void deleteIfUpload(String url) {
        if (url == null) return;

        if (useR2) {
            String prefix = publicUrl + "/";
            if (!url.startsWith(prefix)) return;
            String key = url.substring(prefix.length());
            if (key.isBlank() || key.contains("/") || key.contains("\\")) return;
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (Exception ignored) {
                // best-effort cleanup
            }
            return;
        }

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
