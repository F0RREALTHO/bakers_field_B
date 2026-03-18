package com.bakersfield.web;

import com.bakersfield.model.CustomOrder;
import com.bakersfield.repository.CustomOrderRepository;
import com.bakersfield.service.DataChangePublisher;
import com.bakersfield.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/custom-orders")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class CustomOrdersController {
  private final CustomOrderRepository customOrderRepository;
  private final NotificationService notificationService;
  private final DataChangePublisher dataChangePublisher;

  public CustomOrdersController(CustomOrderRepository customOrderRepository, NotificationService notificationService, DataChangePublisher dataChangePublisher) {
    this.customOrderRepository = customOrderRepository;
    this.notificationService = notificationService;
    this.dataChangePublisher = dataChangePublisher;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CustomOrderResponse placeCustomOrder(@Valid @RequestBody CustomOrderRequest request) {
    CustomOrder order = new CustomOrder();
    order.setCustomerName(request.customerName());
    order.setPhoneNumber(request.phoneNumber());
    order.setDescription(request.description());
    order.setOccasion(request.occasion());
    order.setImageUrl(request.imageUrl());
    order.setEstimatedPriceInr(request.estimatedPriceInr());
    order.setRequestedFor(Instant.now());
    order.setStatus("NEW");
    order.setUpdatedAt(Instant.now());
    CustomOrder saved = customOrderRepository.save(order);
    notificationService.sendCustomOrderNotification(saved);
    dataChangePublisher.publish("custom-orders");
    return new CustomOrderResponse(saved.getId());
  }

  public record CustomOrderRequest(
      @NotBlank String customerName,
      @NotBlank String phoneNumber,
      @NotBlank String description,
      @NotBlank String occasion,
      @NotBlank String imageUrl,
      @NotNull BigDecimal estimatedPriceInr) {
  }

  public record CustomOrderResponse(Long id) {
  }
}
