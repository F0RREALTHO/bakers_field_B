package com.bakersfield.web;

import com.bakersfield.model.Combo;
import com.bakersfield.model.ComboItem;
import com.bakersfield.model.Product;
import com.bakersfield.repository.ComboRepository;
import com.bakersfield.repository.ProductRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
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
@RequestMapping("/api/admin/combos")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminCombosController {
  private final ComboRepository comboRepository;
  private final ProductRepository productRepository;
  private final DataChangePublisher dataChangePublisher;

  public AdminCombosController(ComboRepository comboRepository, ProductRepository productRepository, DataChangePublisher dataChangePublisher) {
    this.comboRepository = comboRepository;
    this.productRepository = productRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping
  public List<ComboResponse> getAll() {
    return comboRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(ComboResponse::from)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ComboResponse create(@Valid @RequestBody ComboRequest request) {
    Combo combo = new Combo();
    apply(combo, request);
    ComboResponse response = ComboResponse.from(comboRepository.save(combo));
    dataChangePublisher.publish("combos");
    return response;
  }

  @PutMapping("/{id}")
  public ComboResponse update(@PathVariable Long id, @Valid @RequestBody ComboRequest request) {
    Combo combo = comboRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo not found"));
    apply(combo, request);
    ComboResponse response = ComboResponse.from(comboRepository.save(combo));
    dataChangePublisher.publish("combos");
    return response;
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!comboRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo not found");
    }
    comboRepository.deleteById(id);
    dataChangePublisher.publish("combos");
  }

  private void apply(Combo combo, ComboRequest request) {
    combo.setName(request.name());
    combo.setDescription(request.description());
    combo.setImageUrl(request.imageUrl());
    combo.setPriceInr(request.priceInr());
    combo.setActive(request.active());

    List<ComboItem> items = request.items().stream().map(item -> {
      Product product = productRepository.findById(item.productId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));
      ComboItem comboItem = new ComboItem();
      comboItem.setCombo(combo);
      comboItem.setProduct(product);
      comboItem.setQuantity(item.quantity());
      return comboItem;
    }).collect(Collectors.toList());

    combo.getItems().clear();
    combo.getItems().addAll(items);
  }

  public record ComboRequest(
      @NotBlank String name,
      @NotNull BigDecimal priceInr,
      String description,
      String imageUrl,
      boolean active,
      @NotNull List<ComboItemRequest> items) {
  }

  public record ComboItemRequest(
      @NotNull Long productId,
      @NotNull Integer quantity) {
  }

  public record ComboResponse(
      Long id,
      String name,
      BigDecimal priceInr,
      String description,
      String imageUrl,
      boolean active,
      List<ComboItemResponse> items) {

    public static ComboResponse from(Combo combo) {
      return new ComboResponse(
          combo.getId(),
          combo.getName(),
          combo.getPriceInr(),
          combo.getDescription(),
          combo.getImageUrl(),
          combo.isActive(),
          combo.getItems().stream().map(ComboItemResponse::from).toList());
    }
  }

  public record ComboItemResponse(
      Long productId,
      String productName,
      Integer quantity) {

    public static ComboItemResponse from(ComboItem item) {
      return new ComboItemResponse(
          item.getProduct().getId(),
          item.getProduct().getName(),
          item.getQuantity());
    }
  }
}
