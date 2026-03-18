package com.bakersfield.model;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "custom_orders")
@Getter
@Setter
@NoArgsConstructor
public class CustomOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String customerName;

  @Column(nullable = false)
  private String phoneNumber;

  @Column(nullable = false, length = 1000)
  private String description;

  @Column
  private String occasion = "General";

  @Column(columnDefinition = "TEXT")
  private String imageUrl;

  @Column(nullable = false)
  private Instant requestedFor = Instant.now();

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal estimatedPriceInr;

  @Column(nullable = false)
  private String status = "NEW";

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();
}
