package com.bakersfield.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final Key signingKey;
  private final long ttlMinutes;

  public JwtService(Environment env) {
    String secret = env.getProperty("jwt.secret", "change-this-secret-to-32-chars-minimum-manually");
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.ttlMinutes = Long.parseLong(env.getProperty("jwt.ttlMinutes", "240"));
  }

  public JwtToken generateToken(String subject) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(ttlMinutes * 60);
    String token = Jwts.builder()
        .setSubject(subject)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expiresAt))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
    return new JwtToken(token, expiresAt);
  }

  public String getSubject(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(signingKey)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public boolean isValid(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  public record JwtToken(String token, Instant expiresAt) {
  }
}
