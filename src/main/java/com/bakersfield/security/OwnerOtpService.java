package com.bakersfield.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OwnerOtpService {
  private static final Duration OTP_TTL = Duration.ofMinutes(5);

  private final SecureRandom random = new SecureRandom();
  private final Map<String, OtpRecord> pendingOtps = new ConcurrentHashMap<>();

  public String generateOtp(String username) {
    int value = random.nextInt(1_000_000);
    String otp = String.format("%06d", value);
    pendingOtps.put(username, new OtpRecord(otp, Instant.now().plus(OTP_TTL)));
    return otp;
  }

  public boolean verifyOtp(String username, String otp) {
    OtpRecord record = pendingOtps.get(username);
    if (record == null) {
      return false;
    }
    if (record.expiresAt().isBefore(Instant.now())) {
      pendingOtps.remove(username);
      return false;
    }
    boolean matched = record.otp().equals(otp);
    if (matched) {
      pendingOtps.remove(username);
    }
    return matched;
  }

  private record OtpRecord(String otp, Instant expiresAt) {
  }
}
