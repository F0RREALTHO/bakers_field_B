package com.bakersfield.repository;

import com.bakersfield.model.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
	@EntityGraph(attributePaths = "items")
	List<Order> findAllByOrderByPlacedAtDesc();

	@EntityGraph(attributePaths = "items")
	Optional<Order> findWithItemsById(Long id);
}
