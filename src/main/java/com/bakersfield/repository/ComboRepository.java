package com.bakersfield.repository;

import com.bakersfield.model.Combo;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboRepository extends JpaRepository<Combo, Long> {
  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Combo> findAllByOrderByCreatedAtDesc();

  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Combo> findByActiveTrueOrderByCreatedAtDesc();
}
