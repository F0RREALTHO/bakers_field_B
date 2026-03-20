package com.bakersfield.security;

import com.bakersfield.model.AdminUser;
import com.bakersfield.repository.AdminUserRepository;
import com.bakersfield.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {
  private final JwtService jwtService;
  private final AdminUserRepository adminUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final OwnerOtpService ownerOtpService;
  private final NotificationService notificationService;
  private final String ownerOtpEmail;

  public AdminAuthController(
      JwtService jwtService,
      AdminUserRepository adminUserRepository,
      PasswordEncoder passwordEncoder,
      OwnerOtpService ownerOtpService,
      NotificationService notificationService,
      @Value("${ADMIN_OWNER_OTP_EMAIL:rashmijee@rediffmail.com}") String ownerOtpEmail) {
    this.jwtService = jwtService;
    this.adminUserRepository = adminUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.ownerOtpService = ownerOtpService;
    this.notificationService = notificationService;
    this.ownerOtpEmail = ownerOtpEmail;
  }

  // Secret owner login route - use this URL in production (only you know this path)
  // URL: POST /api/admin/k9v3p8t7q4n6r1x5m0c2z8h1/login/request-otp
  @PostMapping("/k9v3p8t7q4n6r1x5m0c2z8h1/login/request-otp")
  @ResponseStatus(HttpStatus.OK)
  public OtpRequestResponse secretRequestLoginOtp(@Valid @RequestBody AdminOtpRequest request) {
    AdminUser admin = validateCredentials(request.username(), request.password());
    String otp = ownerOtpService.generateOtp(admin.getUsername());
    notificationService.sendOwnerOtpEmail(ownerOtpEmail, otp);
    return new OtpRequestResponse("OTP sent to owner email");
  }

  // Secret owner login route - complete login with OTP
  // URL: POST /api/admin/k9v3p8t7q4n6r1x5m0c2z8h1/login
  @PostMapping("/k9v3p8t7q4n6r1x5m0c2z8h1/login")
  @ResponseStatus(HttpStatus.OK)
  public AdminLoginResponse secretLogin(@Valid @RequestBody AdminLoginRequest request) {
    AdminUser admin = validateCredentials(request.username(), request.password());
    if (!ownerOtpService.verifyOtp(admin.getUsername(), request.otp())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
    }
    JwtService.JwtToken token = jwtService.generateToken(admin.getUsername());
    return new AdminLoginResponse(token.token(), token.expiresAt());
  }

  private AdminUser validateCredentials(String username, String password) {
    AdminUser admin = adminUserRepository.findByUsername(username)
        .filter(AdminUser::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    return admin;
  }

  public record AdminOtpRequest(@NotBlank String username, @NotBlank String password) {
  }

  public record OtpRequestResponse(String message) {
  }

  public record AdminLoginRequest(@NotBlank String username, @NotBlank String password, @NotBlank String otp) {
  }

  public record AdminLoginResponse(String token, Instant expiresAt) {
  }
}
