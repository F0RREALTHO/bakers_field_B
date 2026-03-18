package com.bakersfield.web;

import com.bakersfield.model.Order;
import com.bakersfield.repository.OrderRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
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
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminOrdersController {
  private final OrderRepository orderRepository;
  private final DataChangePublisher dataChangePublisher;

  public AdminOrdersController(OrderRepository orderRepository, DataChangePublisher dataChangePublisher) {
    this.orderRepository = orderRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping
  public List<OrderResponse> getAllOrders() {
    return orderRepository.findAllByOrderByPlacedAtDesc().stream()
        .map(OrderResponse::from)
        .toList();
  }

  @PatchMapping("/{id}")
  public OrderResponse updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody OrderStatusRequest request) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    order.setStatus(request.status());
    OrderResponse response = OrderResponse.from(orderRepository.save(order));
    dataChangePublisher.publish("orders");
    return response;
  }

  @PatchMapping("/{id}/payment")
  public OrderResponse updatePayment(
      @PathVariable Long id,
      @Valid @RequestBody PaymentStatusRequest request) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    order.setPaymentStatus(request.paymentStatus());
    if (request.paymentReference() != null) {
      order.setPaymentReference(request.paymentReference());
    }
    if (request.paymentProvider() != null) {
      order.setPaymentProvider(request.paymentProvider());
    }
    if (request.paymentMethod() != null) {
      order.setPaymentMethod(request.paymentMethod());
    }
    OrderResponse response = OrderResponse.from(orderRepository.save(order));
    dataChangePublisher.publish("orders");
    return response;
  }

  public record OrderStatusRequest(@NotBlank String status) {
  }

  public record PaymentStatusRequest(
      @NotBlank String paymentStatus,
      String paymentMethod,
      String paymentProvider,
      String paymentReference) {
  }

  public record OrderResponse(
      Long id,
      String customerName,
      String phoneNumber,
      String pinCode,
      String addressLabel,
      String addressLine1,
      String addressLine2,
      String addressCity,
      String addressState,
      java.math.BigDecimal totalAmountInr,
      java.math.BigDecimal subtotalAmountInr,
      java.math.BigDecimal discountAmountInr,
      java.math.BigDecimal saleDiscountAmountInr,
      String saleName,
      String couponCode,
      String paymentMethod,
      String paymentProvider,
      String paymentReference,
      String paymentStatus,
      Integer itemCount,
      List<OrderItemResponse> items,
      String status,
      Instant placedAt) {

    public static OrderResponse from(Order order) {
      return new OrderResponse(
          order.getId(),
          order.getCustomerName(),
          order.getPhoneNumber(),
          order.getPinCode(),
          order.getAddressLabel(),
          order.getAddressLine1(),
          order.getAddressLine2(),
          order.getAddressCity(),
          order.getAddressState(),
          order.getTotalAmountInr(),
          order.getSubtotalAmountInr(),
          order.getDiscountAmountInr(),
          order.getSaleDiscountAmountInr(),
          order.getSaleName(),
          order.getCouponCode(),
          order.getPaymentMethod(),
          order.getPaymentProvider(),
          order.getPaymentReference(),
          order.getPaymentStatus(),
          order.getItemCount(),
          order.getItems().stream().map(OrderItemResponse::from).toList(),
          order.getStatus(),
          order.getPlacedAt());
    }
  }

  public record OrderItemResponse(
      String itemType,
      Long itemRefId,
      String itemName,
      java.math.BigDecimal unitPriceInr,
      Integer quantity,
      java.math.BigDecimal lineTotalInr) {

    public static OrderItemResponse from(com.bakersfield.model.OrderItem item) {
      return new OrderItemResponse(
          item.getItemType(),
          item.getItemRefId(),
          item.getItemName(),
          item.getUnitPriceInr(),
          item.getQuantity(),
          item.getLineTotalInr());
    }
  }
}
