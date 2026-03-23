package com.bakersfield.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal priceInr;

  @Column(precision = 10, scale = 2)
  private BigDecimal originalPriceInr;

  @Column(length = 500)
  private String description;

  @Column(length = 500)
  private String imageUrl;

  @ManyToMany
  @JoinTable(
      name = "product_tag_links",
      joinColumns = @JoinColumn(name = "product_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column
  private Boolean featured = false;

  @ElementCollection
  @CollectionTable(name = "product_ingredients", joinColumns = @JoinColumn(name = "product_id"))
  @Column(name = "ingredient")
  private List<String> ingredients = new ArrayList<>();

  @Column(length = 100)
  private String calories;

  @Column(length = 100)
  private String protein;

  @Column
  private Double weightKg;

  @Column(nullable = false)
  private Boolean active = true;
}
