package com.bakersfield.config;

import java.util.Arrays;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final String[] allowedOriginPatterns;
  private final Path localUploadBasePath;

  public WebConfig(
      @Value("${APP_CORS_ALLOWED_ORIGINS:https://fieldbakers.me,https://www.fieldbakers.me,https://api.fieldbakers.me,http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174}")
      String allowedOriginsRaw,
      @Value("${storage.local.baseDir:uploads}") String localUploadBaseDir) {
    this.allowedOriginPatterns = Arrays.stream(allowedOriginsRaw.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toArray(String[]::new);
    this.localUploadBasePath = Paths.get(localUploadBaseDir).toAbsolutePath().normalize();
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
      .allowedOriginPatterns(allowedOriginPatterns)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uploadLocation = localUploadBasePath.toUri().toString();
    if (!uploadLocation.endsWith("/")) {
        uploadLocation += "/";
    }
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(uploadLocation);
  }
}
