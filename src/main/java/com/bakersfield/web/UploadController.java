package com.bakersfield.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RestController
public class UploadController {
  private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
  private static final long MAX_BYTES = 8L * 1024 * 1024;
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif");

  private final S3Client r2Client;
  private final String bucket;
  private final String publicBaseUrl;
  private final Path localUploadBasePath;
  private final boolean customerLocalFallbackEnabled;

  public UploadController(
      S3Client r2Client,
      @Value("${storage.r2.bucket:}") String bucket,
      @Value("${storage.r2.publicBaseUrl:}") String publicBaseUrl,
      @Value("${storage.local.baseDir:uploads}") String localUploadBaseDir,
      @Value("${storage.upload.customerFallbackEnabled:true}") boolean customerLocalFallbackEnabled) {
    this.r2Client = r2Client;
    this.bucket = bucket;
    this.publicBaseUrl = publicBaseUrl;
    this.localUploadBasePath = Paths.get(localUploadBaseDir).toAbsolutePath().normalize();
    this.customerLocalFallbackEnabled = customerLocalFallbackEnabled;
  }

  @PostMapping(path = "/api/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UploadResponse uploadForCustomer(
      @RequestPart("file") MultipartFile file) {
    validateImage(file);
    String extension = resolveExtension(file.getContentType(), file.getOriginalFilename());
    String normalizedContentType = resolveContentType(file.getContentType(), extension);

    try {
      return uploadToR2(file, normalizedContentType, extension, "custom-orders");
    } catch (ResponseStatusException ex) {
      if (!customerLocalFallbackEnabled) {
        throw ex;
      }
      logger.warn("R2 upload failed, using local fallback for customer upload: {}", ex.getReason());
      String key = writeToLocalStorage(file, extension, "custom-orders-fallback");
      return new UploadResponse(buildLocalPublicUrl(key));
    }
  }

  @PostMapping(path = "/api/admin/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UploadResponse uploadForAdmin(
      @RequestPart("file") MultipartFile file) {
    validateImage(file);
    String extension = resolveExtension(file.getContentType(), file.getOriginalFilename());
    String key = writeToLocalStorage(file, extension, "admin-catalog");
    return new UploadResponse(buildLocalPublicUrl(key));
  }

  private void validateImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be <= 8MB");
    }
    String contentType = file.getContentType();
    if (!isAcceptedImage(file.getOriginalFilename(), contentType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are allowed");
    }
  }

  private UploadResponse uploadToR2(
      MultipartFile file,
      String normalizedContentType,
      String extension,
      String folder) {
    if (bucket.isBlank() || publicBaseUrl.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "R2 storage is not configured");
    }
    String key = folder + "/" + UUID.randomUUID() + extension;

    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
      .contentType(normalizedContentType)
        .build();

    try {
      r2Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed");
    } catch (RuntimeException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Storage upload failed: " + ex.getMessage());
    }

    String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    return new UploadResponse(base + "/" + key);
  }

  private String writeToLocalStorage(MultipartFile file, String extension, String folder) {
    String key = folder + "/" + UUID.randomUUID() + extension;
    Path target = localUploadBasePath.resolve(key).normalize();

    if (!target.startsWith(localUploadBasePath)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload path");
    }

    try {
      Files.createDirectories(target.getParent());
      Files.write(target, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      return key.replace('\\', '/');
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Local upload failed");
    }
  }

  private String buildLocalPublicUrl(String key) {
    return ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/uploads/")
        .path(key)
        .build()
        .toUriString();
  }

  private String resolveExtension(String contentType, String originalName) {
    String normalized = (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT));
    if (normalized.contains("jpeg")) {
      return ".jpg";
    }
    if (normalized.contains("png")) {
      return ".png";
    }
    if (normalized.contains("webp")) {
      return ".webp";
    }
    if (normalized.contains("heic")) {
      return ".heic";
    }
    if (normalized.contains("heif")) {
      return ".heif";
    }
    if (originalName != null && originalName.contains(".")) {
      return originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }
    return ".img";
  }

  private boolean isAcceptedImage(String originalName, String contentType) {
    if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      return true;
    }
    String extension = resolveExtension(contentType, originalName);
    return ALLOWED_EXTENSIONS.contains(extension);
  }

  private String resolveContentType(String contentType, String extension) {
    if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
      return contentType;
    }
    return switch (extension) {
      case ".jpg", ".jpeg" -> "image/jpeg";
      case ".png" -> "image/png";
      case ".webp" -> "image/webp";
      case ".heic" -> "image/heic";
      case ".heif" -> "image/heif";
      default -> "application/octet-stream";
    };
  }

  public record UploadResponse(String url) {
  }
}
