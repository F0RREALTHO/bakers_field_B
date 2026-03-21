package com.bakersfield.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "site_visitors")
@Getter
@Setter
@NoArgsConstructor
public class SiteVisitor {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 120)
  private String visitorId;

  @Column(nullable = false)
  private Instant firstVisitedAt = Instant.now();

  @Column(nullable = false)
  private Instant lastVisitedAt = Instant.now();

  @Column(nullable = false)
  private Long visitCount = 1L;
}