package com.bakersfield.web;

import com.bakersfield.model.SaleConfig;
import com.bakersfield.service.SaleService;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class SalesController {
  private final SaleService saleService;

  public SalesController(SaleService saleService) {
    this.saleService = saleService;
  }

  @GetMapping("/active")
  public SaleResponse getActive() {
    return saleService.getActiveSale().map(SaleResponse::from).orElse(null);
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
