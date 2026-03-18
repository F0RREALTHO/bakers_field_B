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
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private CouponType type;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal minOrderAmount = BigDecimal.ZERO;

  @Column
  private Instant startsAt;

  @Column
  private Instant endsAt;

  @Column
  private Integer usageLimit;

  @Column
  private Integer perCustomerLimit;

  @Column(nullable = false)
  private Integer timesUsed = 0;

  @Column(nullable = false)
  private boolean active = true;

  public enum CouponType {
    PERCENT,
    FLAT
  }
}
