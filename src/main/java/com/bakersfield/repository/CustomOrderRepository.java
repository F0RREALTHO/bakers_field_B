package com.bakersfield.repository;

import com.bakersfield.model.CustomOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomOrderRepository extends JpaRepository<CustomOrder, Long> {
	@Query("select distinct c.phoneNumber from CustomOrder c where c.phoneNumber is not null and c.phoneNumber <> ''")
	List<String> findDistinctPhoneNumbers();
}
