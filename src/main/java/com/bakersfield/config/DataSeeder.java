package com.bakersfield.config;

import com.bakersfield.model.AdminUser;
import com.bakersfield.model.Category;
import com.bakersfield.model.Product;
import com.bakersfield.model.Tag;
import com.bakersfield.repository.AdminUserRepository;
import com.bakersfield.repository.CategoryRepository;
import com.bakersfield.repository.ProductRepository;
import com.bakersfield.repository.TagRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
  @Bean
  CommandLineRunner seedCatalog(
      CategoryRepository categoryRepository,
      ProductRepository productRepository,
      TagRepository tagRepository,
      AdminUserRepository adminUserRepository,
      PasswordEncoder passwordEncoder,
      @Value("${admin.bootstrap.username:admin}") String adminUsername,
      @Value("${admin.bootstrap.password:admin123}") String adminPassword) {
    return args -> {
      if (adminUserRepository.count() == 0) {
        AdminUser admin = new AdminUser();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        adminUserRepository.save(admin);
      }

      Map<String, Tag> commonTags = ensureCommonTags(tagRepository);
      ensureHighlightTags(tagRepository);

      if (categoryRepository.count() > 0 || productRepository.count() > 0) {
        attachCommonTags(productRepository, commonTags.values());
        return;
      }

      Category breads = new Category();
      breads.setName("Artisan Breads");

      Category pastries = new Category();
      pastries.setName("Classic Pastries");

      Category cookies = new Category();
      cookies.setName("Cookies & Treats");

      Category custom = new Category();
      custom.setName("Custom Cakes");

      categoryRepository.saveAll(List.of(breads, pastries, cookies, custom));

      Map<String, Tag> tagCache = new HashMap<>();

      productRepository.saveAll(List.of(
          buildProduct("Classic Sourdough", "36-hour fermented loaf with a crunchy crust.", "https://images.unsplash.com/photo-1509440159596-0249088772ff", BigDecimal.valueOf(280), breads, mergeTags(resolveTags(tagRepository, tagCache, Set.of("artisan", "signature")), commonTags.values())),
          buildProduct("Multi-grain Batard", "Nutty profile with sunflower and sesame seeds.", "https://images.unsplash.com/photo-1549931319-a545dcf3bc73", BigDecimal.valueOf(320), breads, mergeTags(resolveTags(tagRepository, tagCache, Set.of("healthy")), commonTags.values())),
          buildProduct("Garlic Focaccia", "Rosemary infused focaccia with olive oil.", "https://images.unsplash.com/photo-1509440159596-0249088772ff", BigDecimal.valueOf(240), breads, mergeTags(resolveTags(tagRepository, tagCache, Set.of("savory")), commonTags.values())),
          buildProduct("Butter Croissant", "Hand-laminated flaky pastry with French butter.", "https://images.unsplash.com/photo-1509440159596-0249088772ff", BigDecimal.valueOf(145), pastries, mergeTags(resolveTags(tagRepository, tagCache, Set.of("popular")), commonTags.values())),
          buildProduct("Pain au Chocolat", "Flaky layers with dark chocolate center.", "https://images.unsplash.com/photo-1509440159596-0249088772ff", BigDecimal.valueOf(180), pastries, mergeTags(resolveTags(tagRepository, tagCache, Set.of("chocolate")), commonTags.values())),
          buildProduct("Double Choco Donut", "Belgian chocolate ganache and roasted hazelnuts.", "https://images.unsplash.com/photo-1509440159596-0249088772ff", BigDecimal.valueOf(110), pastries, mergeTags(resolveTags(tagRepository, tagCache, Set.of("chocolate")), commonTags.values())),
          buildProduct("Almond Croissant", "Twice-baked with almond frangipane.", "https://images.unsplash.com/photo-1509440159596-0249088772ff", BigDecimal.valueOf(210), pastries, mergeTags(resolveTags(tagRepository, tagCache, Set.of("nutty")), commonTags.values())),
          buildProduct("Walnut Rye", "Dense rye bread packed with toasted walnuts.", "https://images.unsplash.com/photo-1509440159596-0249088772ff", BigDecimal.valueOf(350), breads, mergeTags(resolveTags(tagRepository, tagCache, Set.of("nutty")), commonTags.values())),
          buildProduct("Signature Cookies", "Assorted cookies baked with brown butter.", "https://images.unsplash.com/photo-1509440159596-0249088772ff", BigDecimal.valueOf(120), cookies, mergeTags(resolveTags(tagRepository, tagCache, Set.of("signature")), commonTags.values()))
      ));
    };
  }

  private Product buildProduct(
      String name,
      String description,
      String imageUrl,
      BigDecimal priceInr,
      Category category,
      Set<Tag> tags) {
    Product product = new Product();
    product.setName(name);
    product.setDescription(description);
    product.setImageUrl(imageUrl);
    product.setPriceInr(priceInr);
    product.setCategory(category);
    product.setTags(tags);
    return product;
  }

  private Set<Tag> resolveTags(
      TagRepository tagRepository,
      Map<String, Tag> tagCache,
      Set<String> names) {
    return names.stream()
        .map(name -> tagCache.computeIfAbsent(name, tagName -> {
          String slug = tagName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-")
              .replaceAll("(^-|-$)", "");
          return tagRepository.findBySlug(slug).orElseGet(() -> {
            Tag tag = new Tag();
            tag.setName(tagName.trim());
            tag.setSlug(slug);
            return tagRepository.save(tag);
          });
        }))
        .collect(java.util.stream.Collectors.toSet());
  }

  private Map<String, Tag> ensureCommonTags(TagRepository tagRepository) {
    Map<String, Tag> commonTags = new HashMap<>();
    commonTags.put("handcrafted", upsertTag(tagRepository, "Handcrafted", "#7a3f0c", "#fde6c2"));
    commonTags.put("fresh-baked", upsertTag(tagRepository, "Fresh Baked", "#7a3f0c", "#fde6c2"));
    commonTags.put("100-veg", upsertTag(tagRepository, "100% Veg", "#1f7a3c", "#e1f7e8"));
    return commonTags;
  }

  private void ensureHighlightTags(TagRepository tagRepository) {
    upsertTag(tagRepository, "Trending", "#b91c1c", "#fee2e2");
    upsertTag(tagRepository, "New", "#1d4ed8", "#dbeafe");
  }

  private Tag upsertTag(
      TagRepository tagRepository,
      String name,
      String textColor,
      String backgroundColor) {
    String slug = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    return tagRepository.findBySlug(slug).orElseGet(() -> {
      Tag tag = new Tag();
      tag.setName(name.trim());
      tag.setSlug(slug);
      tag.setTextColor(textColor);
      tag.setBackgroundColor(backgroundColor);
      return tagRepository.save(tag);
    });
  }

  private Set<Tag> mergeTags(Set<Tag> tags, java.util.Collection<Tag> commonTags) {
    java.util.Set<Tag> merged = new java.util.HashSet<>(tags);
    merged.addAll(commonTags);
    return merged;
  }

  private void attachCommonTags(ProductRepository productRepository, java.util.Collection<Tag> commonTags) {
    List<Product> products = productRepository.findAllByOrderByIdAsc();
    boolean updated = false;
    for (Product product : products) {
      if (!product.getTags().containsAll(commonTags)) {
        product.getTags().addAll(commonTags);
        updated = true;
      }
    }
    if (updated) {
      productRepository.saveAll(products);
    }
  }
}
