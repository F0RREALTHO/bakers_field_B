package com.bakersfield.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class R2Config {
  @Bean
  public S3Client r2Client(
      @Value("${storage.r2.endpoint:}") String endpoint,
      @Value("${storage.r2.accessKey:}") String accessKey,
      @Value("${storage.r2.secretKey:}") String secretKey) {
    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
    return S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .endpointOverride(URI.create(endpoint))
        .region(Region.of("auto"))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
