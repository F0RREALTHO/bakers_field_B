package com.bakersfield.security;

import com.bakersfield.model.AdminUser;
import com.bakersfield.repository.AdminUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminAuthController {
  private final JwtService jwtService;
  private final AdminUserRepository adminUserRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminAuthController(
      JwtService jwtService,
      AdminUserRepository adminUserRepository,
      PasswordEncoder passwordEncoder) {
    this.jwtService = jwtService;
    this.adminUserRepository = adminUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
    AdminUser admin = adminUserRepository.findByUsername(request.username())
        .filter(AdminUser::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    JwtService.JwtToken token = jwtService.generateToken(admin.getUsername());
    return new AdminLoginResponse(token.token(), token.expiresAt());
  }

  public record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {
  }

  public record AdminLoginResponse(String token, Instant expiresAt) {
  }
}
