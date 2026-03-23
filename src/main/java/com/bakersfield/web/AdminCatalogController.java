package com.bakersfield.web;

import com.bakersfield.model.Category;
import com.bakersfield.model.Product;
import com.bakersfield.model.Tag;
import com.bakersfield.repository.CategoryRepository;
import com.bakersfield.repository.ProductRepository;
import com.bakersfield.repository.TagRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminCatalogController {
  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final TagRepository tagRepository;
  private final DataChangePublisher dataChangePublisher;

  public AdminCatalogController(
      ProductRepository productRepository,
      CategoryRepository categoryRepository,
      TagRepository tagRepository,
      DataChangePublisher dataChangePublisher) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.tagRepository = tagRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping("/products")
  public List<ProductResponse> getProducts() {
    return productRepository.findAllByOrderByNameAsc().stream()
        .map(ProductResponse::from)
        .toList();
  }

  @jakarta.transaction.Transactional
  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
    Category category = categoryRepository.findById(request.categoryId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    Product product = new Product();
    applyProduct(product, request, category);
    ProductResponse response = ProductResponse.from(productRepository.save(product));
    dataChangePublisher.publish("products");
    return response;
  }

  @jakarta.transaction.Transactional
  @PutMapping("/products/{id}")
  public ProductResponse updateProduct(
      @PathVariable Long id,
      @Valid @RequestBody ProductRequest request) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    Category category = categoryRepository.findById(request.categoryId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    applyProduct(product, request, category);
    ProductResponse response = ProductResponse.from(productRepository.save(product));
    dataChangePublisher.publish("products");
    return response;
  }

  @DeleteMapping("/products/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProduct(@PathVariable long id) {
    if (!productRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
    }
    productRepository.deleteById(id);
    dataChangePublisher.publish("products");
  }

  @Transactional
  @PutMapping("/products/{id}/featured")
  public ProductResponse toggleFeatured(
      @PathVariable Long id,
      @RequestBody FeaturedRequest request) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    product.setFeatured(request.featured());
    Product saved = productRepository.saveAndFlush(product);
    dataChangePublisher.publish("products");
    return ProductResponse.from(saved);
  }

  @GetMapping("/categories")
  public List<CategoryResponse> getCategories() {
    return categoryRepository.findAll(Sort.by("name")).stream()
        .map(category -> new CategoryResponse(category.getId(), category.getName()))
        .toList();
  }

  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
    Category category = new Category();
    category.setName(request.name());
    CategoryResponse response = new CategoryResponse(categoryRepository.save(category).getId(), category.getName());
    dataChangePublisher.publish("categories");
    return response;
  }

  @PutMapping("/categories/{id}")
  public CategoryResponse updateCategory(
      @PathVariable Long id,
      @Valid @RequestBody CategoryRequest request) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    category.setName(request.name());
    CategoryResponse response = new CategoryResponse(categoryRepository.save(category).getId(), category.getName());
    dataChangePublisher.publish("categories");
    return response;
  }

  @DeleteMapping("/categories/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCategory(@PathVariable Long id) {
    Optional<Category> category = categoryRepository.findById(id);
    if (category.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
    }
    boolean hasProducts = productRepository.findByCategoryId(id).stream().findAny().isPresent();
    if (hasProducts) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Category has products");
    }
    categoryRepository.deleteById(id);
    dataChangePublisher.publish("categories");
  }

  private void applyProduct(Product product, ProductRequest request, Category category) {
    product.setName(request.name());
    product.setPriceInr(request.priceInr());
    product.setOriginalPriceInr(request.originalPriceInr());
    product.setDescription(request.description());
    product.setImageUrl(request.imageUrl());
    Set<Tag> tags = new HashSet<>(resolveTags(request.tagIds()));
    product.setTags(tags);
    product.setCategory(category);
    product.setIngredients(request.ingredients() != null ? new java.util.ArrayList<>(request.ingredients()) : new java.util.ArrayList<>());
    product.setCalories(request.calories());
    product.setProtein(request.protein());
    product.setWeightKg(request.weightKg());
    if (request.active() != null) {
      product.setActive(request.active());
    } else if (product.getActive() == null) {
      product.setActive(true);
    }
    if (request.featured() != null) {
      product.setFeatured(request.featured());
    }
  }

  private Set<Tag> resolveCommonTags() {
    Set<Tag> commonTags = new HashSet<>();
    commonTags.add(upsertTag("Handcrafted", "#7a3f0c", "#fde6c2"));
    commonTags.add(upsertTag("Fresh Baked", "#7a3f0c", "#fde6c2"));
    commonTags.add(upsertTag("100% Veg", "#1f7a3c", "#e1f7e8"));
    return commonTags;
  }

  private Tag upsertTag(String name, String textColor, String backgroundColor) {
    String slug = normalizeSlug(name);
    return tagRepository.findBySlug(slug).orElseGet(() -> {
      Tag tag = new Tag();
      tag.setName(name.trim());
      tag.setSlug(slug);
      tag.setTextColor(textColor);
      tag.setBackgroundColor(backgroundColor);
      return tagRepository.save(tag);
    });
  }

  private String normalizeSlug(String name) {
    return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
  }

  private Set<Tag> resolveTags(List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return Set.of();
    }
    List<Tag> tags = tagRepository.findAllById(tagIds);
    if (tags.size() != tagIds.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tags not found");
    }
    return tags.stream().collect(Collectors.toSet());
  }

  public record ProductRequest(
      @NotBlank String name,
      @NotNull BigDecimal priceInr,
      BigDecimal originalPriceInr,
      String description,
      String imageUrl,
      List<Long> tagIds,
      @NotNull Long categoryId,
      Boolean featured,
      List<String> ingredients,
      String calories,
      String protein,
      Double weightKg,
      Boolean active) {
  }

  public record FeaturedRequest(boolean featured) {
  }

  public record ProductResponse(
      Long id,
      String name,
      BigDecimal priceInr,
      BigDecimal originalPriceInr,
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

    public static ProductResponse from(Product product) {
      return new ProductResponse(
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
          product.getIngredients(),
          product.getCalories(),
          product.getProtein(),
          product.getWeightKg(),
          Boolean.TRUE.equals(product.getActive()));
    }
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

  public record CategoryRequest(@NotBlank String name) {
  }

  public record CategoryResponse(Long id, String name) {
  }
}
