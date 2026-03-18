package com.bakersfield.web;

import com.bakersfield.model.Review;
import com.bakersfield.repository.ReviewRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class ReviewsController {
  private final ReviewRepository reviewRepository;
  private final DataChangePublisher dataChangePublisher;

  public ReviewsController(ReviewRepository reviewRepository, DataChangePublisher dataChangePublisher) {
    this.reviewRepository = reviewRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping
  public List<ReviewResponse> getReviews(
      @RequestParam(required = false) Boolean featured,
      @RequestParam(required = false) String authorPhone,
      @RequestParam(required = false) String authorName) {
    List<Review> reviews;
    if (authorPhone != null && !authorPhone.isBlank()) {
      reviews = reviewRepository.findAllByAuthorPhoneOrderByCreatedAtDesc(authorPhone);
    } else if (authorName != null && !authorName.isBlank()) {
      reviews = reviewRepository.findAllByAuthorNameIgnoreCaseOrderByCreatedAtDesc(authorName);
    } else if (Boolean.TRUE.equals(featured)) {
      reviews = reviewRepository.findAllByApprovedTrueAndFeaturedTrueOrderByCreatedAtDesc();
    } else {
      reviews = reviewRepository.findAllByApprovedTrueOrderByCreatedAtDesc();
    }

    return reviews.stream().map(ReviewResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReviewResponse submitReview(@Valid @RequestBody ReviewRequest request) {
    Review review = new Review();
    review.setProductId(request.productId());
    review.setProductName(request.productName());
    review.setRating(request.rating());
    review.setComment(request.comment());
    review.setAuthorName(request.authorName());
    review.setAuthorPhone(request.authorPhone());
    review.setAvatar(request.avatar());
    review.setApproved(false);
    review.setFeatured(false);
    review.setCreatedAt(Instant.now());
    Review saved = reviewRepository.save(review);
    dataChangePublisher.publish("reviews");
    return ReviewResponse.from(saved);
  }

  public record ReviewRequest(
      @NotNull Long productId,
      @NotBlank String productName,
      @Min(1) @Max(5) Integer rating,
      @NotBlank String comment,
      @NotBlank String authorName,
      String authorPhone,
      @NotBlank String avatar) {
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
