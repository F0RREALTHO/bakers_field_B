package com.bakersfield.repository;

import com.bakersfield.model.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {
	@EntityGraph(attributePaths = "items")
	List<Order> findAllByOrderByPlacedAtDesc();

	@EntityGraph(attributePaths = "items")
	Optional<Order> findWithItemsById(Long id);

	@Query("select coalesce(sum(o.totalAmountInr), 0) from Order o")
	BigDecimal sumTotalRevenue();

	@Query("select distinct o.phoneNumber from Order o where o.phoneNumber is not null and o.phoneNumber <> ''")
	List<String> findDistinctPhoneNumbers();
}
