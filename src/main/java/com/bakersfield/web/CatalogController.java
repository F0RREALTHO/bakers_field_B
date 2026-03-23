package com.bakersfield.web;

import com.bakersfield.model.Product;
import com.bakersfield.model.Tag;
import com.bakersfield.repository.CategoryRepository;
import com.bakersfield.repository.ProductRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class CatalogController {
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  public CatalogController(CategoryRepository categoryRepository, ProductRepository productRepository) {
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
  }

  @GetMapping("/categories")
  public List<CategoryResponse> getCategories() {
    return categoryRepository.findAll(Sort.by("name")).stream()
        .map(category -> new CategoryResponse(category.getId(), category.getName()))
        .toList();
  }

  @GetMapping("/products")
  @Transactional
  public List<ProductResponse> getProducts(@RequestParam Optional<Long> categoryId) {
    List<Product> products = categoryId
      .map(productRepository::findByCategoryId)
      .orElseGet(productRepository::findAllByOrderByIdAsc);

    return products.stream()
        .filter(product -> Boolean.TRUE.equals(product.getActive()))
        .map(product -> new ProductResponse(
          product.getId(),
          product.getName(),
          product.getPriceInr(),
          product.getOriginalPriceInr(),
          product.getDescription(),
          product.getImageUrl(),
          product.getTags().stream().map(TagResponse::from).toList(),
          product.getCategory().getId(),
          product.getCategory().getName(),
          Boolean.TRUE.equals(product.getFeatured()),
          new java.util.ArrayList<>(product.getIngredients()),
          product.getCalories(),
          product.getProtein(),
          product.getWeightKg(),
          Boolean.TRUE.equals(product.getActive())))
        .toList();
  }

  public record CategoryResponse(Long id, String name) {
  }

  public record ProductResponse(
      Long id,
      String name,
      java.math.BigDecimal priceInr,
      java.math.BigDecimal originalPriceInr,
      String description,
      String imageUrl,
      List<TagResponse> tags,
      Long categoryId,
      String categoryName,
      boolean featured,
      List<String> ingredients,
      String calories,
      String protein,
      Double weightKg,
      boolean active) {
  }

  public record TagResponse(
      Long id,
      String name,
      String slug,
      String textColor,
      String backgroundColor) {

    public static TagResponse from(Tag tag) {
      return new TagResponse(
          tag.getId(),
          tag.getName(),
          tag.getSlug(),
          tag.getTextColor(),
          tag.getBackgroundColor());
    }
  }
}
