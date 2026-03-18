package com.bakersfield.web;

import com.bakersfield.model.CustomOrder;
import com.bakersfield.repository.CustomOrderRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/custom-orders")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminCustomOrdersController {
  private final CustomOrderRepository customOrderRepository;
  private final DataChangePublisher dataChangePublisher;

  public AdminCustomOrdersController(CustomOrderRepository customOrderRepository, DataChangePublisher dataChangePublisher) {
    this.customOrderRepository = customOrderRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping
  public List<CustomOrderResponse> getCustomOrders() {
    return customOrderRepository.findAll(Sort.by(Sort.Direction.DESC, "requestedFor")).stream()
        .map(CustomOrderResponse::from)
        .toList();
  }

  @PatchMapping("/{id}")
  public CustomOrderResponse updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody CustomOrderStatusRequest request) {
    CustomOrder order = customOrderRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom order not found"));
    order.setStatus(request.status());
    order.setUpdatedAt(Instant.now());
    CustomOrderResponse response = CustomOrderResponse.from(customOrderRepository.save(order));
    dataChangePublisher.publish("custom-orders");
    return response;
  }

  public record CustomOrderStatusRequest(@NotBlank String status) {
  }

  public record CustomOrderResponse(
      Long id,
      String customerName,
      String phoneNumber,
      String description,
      String occasion,
      String imageUrl,
      java.math.BigDecimal estimatedPriceInr,
      String status,
      Instant requestedFor,
      Instant updatedAt) {

    public static CustomOrderResponse from(CustomOrder order) {
      return new CustomOrderResponse(
          order.getId(),
          order.getCustomerName(),
          order.getPhoneNumber(),
          order.getDescription(),
          order.getOccasion(),
          order.getImageUrl(),
          order.getEstimatedPriceInr(),
          order.getStatus(),
          order.getRequestedFor(),
          order.getUpdatedAt());
    }
  }
}
