package com.bakersfield.repository;

import com.bakersfield.model.Product;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
  @EntityGraph(attributePaths = {"tags", "category"})
  List<Product> findAllByOrderByNameAsc();

  @EntityGraph(attributePaths = {"tags", "category"})
  List<Product> findByCategoryId(Long categoryId);

  @EntityGraph(attributePaths = {"tags", "category"})
  List<Product> findAllByOrderByIdAsc();
}
