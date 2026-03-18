package com.bakersfield.repository;

import com.bakersfield.model.CustomOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomOrderRepository extends JpaRepository<CustomOrder, Long> {
}
