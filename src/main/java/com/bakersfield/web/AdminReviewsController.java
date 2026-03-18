package com.bakersfield.web;

import com.bakersfield.model.Review;
import com.bakersfield.repository.ReviewRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/reviews")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminReviewsController {
  private final ReviewRepository reviewRepository;
  private final DataChangePublisher dataChangePublisher;

  public AdminReviewsController(ReviewRepository reviewRepository, DataChangePublisher dataChangePublisher) {
    this.reviewRepository = reviewRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping
  public List<ReviewResponse> getAllReviews() {
    return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(ReviewResponse::from)
        .toList();
  }

  @PatchMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ReviewResponse updateReview(
      @PathVariable Long id,
      @Valid @RequestBody ReviewUpdateRequest request) {
    Review review = reviewRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

    if (request.approved() != null) {
      review.setApproved(request.approved());
      if (!request.approved()) {
        review.setFeatured(false);
      }
    }
    if (request.featured() != null) {
      review.setFeatured(request.featured());
      if (request.featured()) {
        review.setApproved(true);
      }
    }
    review.setCreatedAt(review.getCreatedAt() == null ? Instant.now() : review.getCreatedAt());
    Review saved = reviewRepository.save(review);
    ReviewResponse response = ReviewResponse.from(saved);
    dataChangePublisher.publish("reviews");
    return response;
  }

  public record ReviewUpdateRequest(
      Boolean approved,
      Boolean featured) {
  }

  public record ReviewResponse(
      Long id,
      Long productId,
      String productName,
      Integer rating,
      String comment,
      String authorName,
      String authorPhone,
      String avatar,
      boolean approved,
      boolean featured,
      Instant createdAt) {

    public static ReviewResponse from(Review review) {
      return new ReviewResponse(
          review.getId(),
          review.getProductId(),
          review.getProductName(),
          review.getRating(),
          review.getComment(),
          review.getAuthorName(),
          review.getAuthorPhone(),
          review.getAvatar(),
          review.isApproved(),
          review.isFeatured(),
          review.getCreatedAt());
    }
  }
}
