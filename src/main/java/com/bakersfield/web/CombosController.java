package com.bakersfield.web;

import com.bakersfield.model.Combo;
import com.bakersfield.model.ComboItem;
import com.bakersfield.repository.ComboRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/combos")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class CombosController {
  private final ComboRepository comboRepository;

  public CombosController(ComboRepository comboRepository) {
    this.comboRepository = comboRepository;
  }

  @GetMapping
  public List<ComboResponse> getActiveCombos() {
    return comboRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
        .map(ComboResponse::from)
        .toList();
  }

  public record ComboResponse(
      Long id,
      String name,
      BigDecimal priceInr,
      String description,
      String imageUrl,
      List<ComboItemResponse> items) {

    public static ComboResponse from(Combo combo) {
      return new ComboResponse(
          combo.getId(),
          combo.getName(),
          combo.getPriceInr(),
          combo.getDescription(),
          combo.getImageUrl(),
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
