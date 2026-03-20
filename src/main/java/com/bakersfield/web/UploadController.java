package com.bakersfield.web;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
  private static final long MAX_BYTES = 8L * 1024 * 1024;

  private final S3Client r2Client;
  private final String bucket;
  private final String publicBaseUrl;

  public UploadController(
      S3Client r2Client,
      @Value("${storage.r2.bucket:}") String bucket,
      @Value("${storage.r2.publicBaseUrl:}") String publicBaseUrl) {
    this.r2Client = r2Client;
    this.bucket = bucket;
    this.publicBaseUrl = publicBaseUrl;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UploadResponse upload(@RequestPart("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be <= 8MB");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are allowed");
    }
    if (bucket.isBlank() || publicBaseUrl.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "R2 storage is not configured");
    }

    String extension = resolveExtension(contentType, file.getOriginalFilename());
    String key = "custom-orders/" + UUID.randomUUID() + extension;

    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType(contentType)
        .build();

    try {
      r2Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed");
    } catch (RuntimeException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Storage upload failed");
    }

    String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    return new UploadResponse(base + "/" + key);
  }

  private String resolveExtension(String contentType, String originalName) {
    String normalized = contentType.toLowerCase(Locale.ROOT);
    if (normalized.contains("jpeg")) {
      return ".jpg";
    }
    if (normalized.contains("png")) {
      return ".png";
    }
    if (normalized.contains("webp")) {
      return ".webp";
    }
    if (originalName != null && originalName.contains(".")) {
      return originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }
    return ".img";
  }

  public record UploadResponse(String url) {
  }
}
