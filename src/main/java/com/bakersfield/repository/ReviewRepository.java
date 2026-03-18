package com.bakersfield.repository;

import com.bakersfield.model.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  List<Review> findAllByOrderByCreatedAtDesc();

  List<Review> findAllByApprovedTrueOrderByCreatedAtDesc();

  List<Review> findAllByApprovedTrueAndFeaturedTrueOrderByCreatedAtDesc();

  List<Review> findAllByAuthorPhoneOrderByCreatedAtDesc(String authorPhone);

  List<Review> findAllByAuthorNameIgnoreCaseOrderByCreatedAtDesc(String authorName);
}
