package com.bakersfield.service;

import com.bakersfield.model.SaleConfig;
import com.bakersfield.repository.SaleConfigRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SaleService {
  private final SaleConfigRepository saleConfigRepository;

  public SaleService(SaleConfigRepository saleConfigRepository) {
    this.saleConfigRepository = saleConfigRepository;
  }

  public Optional<SaleConfig> getActiveSale() {
    return saleConfigRepository.findFirstByOrderByIdAsc()
        .filter(SaleConfig::isActive)
        .filter(config -> {
          Instant now = Instant.now();
          return (config.getStartsAt() == null || !now.isBefore(config.getStartsAt()))
              && (config.getEndsAt() == null || !now.isAfter(config.getEndsAt()));
        });
  }

  public SaleResult applySale(BigDecimal subtotalAmountInr) {
    if (subtotalAmountInr == null || subtotalAmountInr.compareTo(BigDecimal.ZERO) <= 0) {
      return new SaleResult(null, BigDecimal.ZERO);
    }
    Optional<SaleConfig> activeSale = getActiveSale();
    if (activeSale.isEmpty()) {
      return new SaleResult(null, BigDecimal.ZERO);
    }

    SaleConfig sale = activeSale.get();
    BigDecimal discount = switch (sale.getType()) {
      case PERCENT -> subtotalAmountInr
          .multiply(sale.getAmount())
          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
          .min(subtotalAmountInr);
      case FLAT -> sale.getAmount().min(subtotalAmountInr);
    };

    return new SaleResult(sale, discount.setScale(2, RoundingMode.HALF_UP));
  }

  public record SaleResult(SaleConfig sale, BigDecimal discountAmountInr) {
  }
}
