package com.bakersfield.web;

import com.bakersfield.model.Order;
import com.bakersfield.model.OrderItem;
import com.bakersfield.repository.OrderRepository;
import com.bakersfield.service.CouponService;
import com.bakersfield.service.DataChangePublisher;
import com.bakersfield.service.NotificationService;
import com.bakersfield.service.SaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class OrdersController {
  private final OrderRepository orderRepository;
  private final CouponService couponService;
  private final SaleService saleService;
  private final NotificationService notificationService;
  private final DataChangePublisher dataChangePublisher;

  public OrdersController(
      OrderRepository orderRepository,
      CouponService couponService,
      SaleService saleService,
      NotificationService notificationService,
      DataChangePublisher dataChangePublisher) {
    this.orderRepository = orderRepository;
    this.couponService = couponService;
    this.saleService = saleService;
    this.notificationService = notificationService;
    this.dataChangePublisher = dataChangePublisher;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse placeOrder(@Valid @RequestBody OrderRequest request) {
    if (request.items() == null || request.items().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order items are required");
    }
    Order order = new Order();
    order.setCustomerName(request.customerName());
    order.setPhoneNumber(request.phoneNumber());
    order.setPinCode(request.pinCode());
    order.setAddressLabel(request.addressLabel());
    order.setAddressLine1(request.addressLine1());
    order.setAddressLine2(request.addressLine2());
    order.setAddressCity(request.addressCity());
    order.setAddressState(request.addressState());
    order.setPaymentMethod(request.paymentMethod());
    order.setPaymentProvider(request.paymentProvider());
    order.setPaymentReference(request.paymentReference());
    order.setPaymentStatus(request.paymentStatus());
    BigDecimal subtotal = request.subtotalAmountInr() != null
        ? request.subtotalAmountInr()
        : request.totalAmountInr();
    var saleResult = saleService.applySale(subtotal);
    BigDecimal saleDiscount = saleResult.discountAmountInr();
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal finalAmount = subtotal.subtract(saleDiscount).max(BigDecimal.ZERO);

    if (request.couponCode() != null && !request.couponCode().isBlank()) {
      var result = couponService.applyCoupon(
          request.couponCode(),
          subtotal,
          request.phoneNumber());
      discount = result.discountAmountInr();
      finalAmount = finalAmount.subtract(discount).max(BigDecimal.ZERO);
      order.setCouponCode(result.code());
    }

    order.setSubtotalAmountInr(subtotal);
    order.setDiscountAmountInr(discount);
    order.setSaleDiscountAmountInr(saleDiscount);
    order.setSaleName(saleResult.sale() != null ? saleResult.sale().getName() : null);
    order.setTotalAmountInr(finalAmount);
    int itemCount = request.items().stream().mapToInt(OrderItemRequest::quantity).sum();
    order.setItemCount(itemCount);
    if (request.placedAt() != null) {
      order.setPlacedAt(request.placedAt());
    } else {
      order.setPlacedAt(Instant.now());
    }

    List<OrderItem> items = request.items().stream().map(item -> {
      OrderItem orderItem = new OrderItem();
      orderItem.setOrder(order);
      orderItem.setItemType(item.itemType());
      orderItem.setItemRefId(item.itemRefId());
      orderItem.setItemName(item.itemName());
      orderItem.setUnitPriceInr(item.unitPriceInr());
      orderItem.setQuantity(item.quantity());
      orderItem.setLineTotalInr(item.unitPriceInr()
          .multiply(BigDecimal.valueOf(item.quantity())));
      return orderItem;
    }).collect(Collectors.toList());

    order.setItems(items);
    Order saved = orderRepository.save(order);
    notificationService.sendOrderNotification(saved);
    dataChangePublisher.publish("orders");
    return OrderResponse.from(saved);
  }

  @GetMapping("/{id}")
  public OrderResponse getOrder(@PathVariable Long id) {
    Order order = orderRepository.findWithItemsById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    return OrderResponse.from(order);
  }

  public record OrderRequest(
      @NotBlank String customerName,
      @NotBlank String phoneNumber,
      @Pattern(regexp = "\\d{6}") String pinCode,
      String addressLabel,
      String addressLine1,
      String addressLine2,
      String addressCity,
      String addressState,
      @NotNull BigDecimal totalAmountInr,
      BigDecimal subtotalAmountInr,
      String couponCode,
      @NotNull Integer itemCount,
      @NotBlank String paymentMethod,
      String paymentProvider,
      String paymentReference,
      @NotBlank String paymentStatus,
      Instant placedAt,
      @NotNull List<OrderItemRequest> items) {
    }

    public record OrderItemRequest(
      @NotBlank String itemType,
      @NotNull Long itemRefId,
      @NotBlank String itemName,
      @NotNull BigDecimal unitPriceInr,
      @NotNull Integer quantity) {
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
      BigDecimal totalAmountInr,
      BigDecimal subtotalAmountInr,
      BigDecimal discountAmountInr,
      BigDecimal saleDiscountAmountInr,
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
      BigDecimal unitPriceInr,
      Integer quantity,
      BigDecimal lineTotalInr) {

    public static OrderItemResponse from(OrderItem item) {
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
