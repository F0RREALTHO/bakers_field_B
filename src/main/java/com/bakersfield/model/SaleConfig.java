package com.bakersfield.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sale_configs")
@Getter
@Setter
@NoArgsConstructor
public class SaleConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private SaleType type;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column
  private Instant startsAt;

  @Column
  private Instant endsAt;

  @Column(nullable = false)
  private boolean active = true;

  public enum SaleType {
    PERCENT,
    FLAT
  }
}
