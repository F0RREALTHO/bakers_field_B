package com.bakersfield.web;

import com.bakersfield.model.SaleConfig;
import com.bakersfield.repository.SaleConfigRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sales")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminSalesController {
  private final SaleConfigRepository saleConfigRepository;
  private final DataChangePublisher dataChangePublisher;

  public AdminSalesController(SaleConfigRepository saleConfigRepository, DataChangePublisher dataChangePublisher) {
    this.saleConfigRepository = saleConfigRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping("/current")
  public SaleResponse getCurrent() {
    return saleConfigRepository.findFirstByOrderByIdAsc()
        .map(SaleResponse::from)
        .orElse(null);
  }

  @PutMapping("/current")
  public SaleResponse update(@Valid @RequestBody SaleRequest request) {
    SaleConfig config = saleConfigRepository.findFirstByOrderByIdAsc()
        .orElseGet(SaleConfig::new);
    config.setName(request.name());
    config.setType(request.type());
    config.setAmount(request.amount());
    config.setStartsAt(request.startsAt());
    config.setEndsAt(request.endsAt());
    config.setActive(request.active());
    SaleResponse response = SaleResponse.from(saleConfigRepository.save(config));
    dataChangePublisher.publish("sales");
    return response;
  }

  public record SaleRequest(
      @NotBlank String name,
      @NotNull SaleConfig.SaleType type,
      @NotNull BigDecimal amount,
      Instant startsAt,
      Instant endsAt,
      boolean active) {
  }

  public record SaleResponse(
      Long id,
      String name,
      SaleConfig.SaleType type,
      BigDecimal amount,
      Instant startsAt,
      Instant endsAt,
      boolean active) {

    public static SaleResponse from(SaleConfig config) {
      return new SaleResponse(
          config.getId(),
          config.getName(),
          config.getType(),
          config.getAmount(),
          config.getStartsAt(),
          config.getEndsAt(),
          config.isActive());
    }
  }
}
