package com.bakersfield.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String customerName;

  @Column(nullable = false)
  private String phoneNumber;

  @Pattern(regexp = "\\d{6}")
  @Column(nullable = false, length = 6)
  private String pinCode;

  @Column(length = 20)
  private String addressLabel;

  @Column(length = 160)
  private String addressLine1;

  @Column(length = 160)
  private String addressLine2;

  @Column(length = 80)
  private String addressCity;

  @Column(length = 80)
  private String addressState;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal totalAmountInr;

  @Column(precision = 10, scale = 2)
  private BigDecimal subtotalAmountInr = BigDecimal.ZERO;

  @Column(precision = 10, scale = 2)
  private BigDecimal discountAmountInr = BigDecimal.ZERO;

  @Column(precision = 10, scale = 2)
  private BigDecimal saleDiscountAmountInr = BigDecimal.ZERO;

  @Column(length = 120)
  private String saleName;

  @Column(length = 32)
  private String couponCode;

  @Column(length = 20)
  private String paymentMethod;

  @Column(length = 60)
  private String paymentProvider;

  @Column(length = 80)
  private String paymentReference;

  @Column(length = 20)
  private String paymentStatus = "PENDING";

  @Column(nullable = false)
  private Integer itemCount;

  @Column(nullable = false)
  private String status = "PLACED";

  @Column(nullable = false)
  private Instant placedAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();
}
