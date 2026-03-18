package com.bakersfield.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long productId;

  @Column(nullable = false, length = 120)
  private String productName;

  @Min(1)
  @Max(5)
  @Column(nullable = false)
  private Integer rating;

  @Column(nullable = false, length = 500)
  private String comment;

  @Column(nullable = false, length = 80)
  private String authorName;

  @Column(length = 20)
  private String authorPhone;

  @Column(nullable = false, length = 200)
  private String avatar;

  @Column(nullable = false)
  private boolean approved = false;

  @Column(nullable = false)
  private boolean featured = false;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();
}
