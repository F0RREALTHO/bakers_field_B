package com.bakersfield.repository;

import com.bakersfield.model.SaleConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleConfigRepository extends JpaRepository<SaleConfig, Long> {
  Optional<SaleConfig> findFirstByOrderByIdAsc();
}
