package com.bakersfield.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.Set;
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

@RestController
public class UploadController {
  private static final long MAX_BYTES = 8L * 1024 * 1024;
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
      ".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif", ".avif", ".bmp", ".gif", ".tif", ".tiff");

  private final Path localUploadBasePath;
  private final String localPublicBaseUrl;

  public UploadController(
      @Value("${storage.local.baseDir:uploads}") String localUploadBaseDir,
      @Value("${storage.local.publicBaseUrl:}") String localPublicBaseUrl) {
    this.localUploadBasePath = Paths.get(localUploadBaseDir).toAbsolutePath().normalize();
    this.localPublicBaseUrl = localPublicBaseUrl == null ? "" : localPublicBaseUrl;
  }

  @PostMapping(path = "/api/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UploadResponse uploadForCustomer(
      @RequestPart("file") MultipartFile file) {
    validateImage(file);
    String extension = resolveExtension(file.getContentType(), file.getOriginalFilename());
    String key = writeToLocalStorage(file, extension, "custom-orders");
    return new UploadResponse(buildLocalPublicUrl(key));
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
    if (!localPublicBaseUrl.isBlank()) {
      String base = localPublicBaseUrl.endsWith("/")
          ? localPublicBaseUrl.substring(0, localPublicBaseUrl.length() - 1)
          : localPublicBaseUrl;
      return base + "/uploads/" + key;
    }

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
    if (normalized.contains("avif")) {
      return ".avif";
    }
    if (normalized.contains("bmp")) {
      return ".bmp";
    }
    if (normalized.contains("gif")) {
      return ".gif";
    }
    if (normalized.contains("tif")) {
      return ".tif";
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

  public record UploadResponse(String url) {
  }
}
