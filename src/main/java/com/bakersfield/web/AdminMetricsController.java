package com.bakersfield.web;

import com.bakersfield.repository.CustomOrderRepository;
import com.bakersfield.repository.OrderRepository;
import com.bakersfield.repository.SiteVisitorRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/metrics")
public class AdminMetricsController {
  private final OrderRepository orderRepository;
  private final CustomOrderRepository customOrderRepository;
  private final SiteVisitorRepository siteVisitorRepository;

  public AdminMetricsController(
      OrderRepository orderRepository,
      CustomOrderRepository customOrderRepository,
      SiteVisitorRepository siteVisitorRepository) {
    this.orderRepository = orderRepository;
    this.customOrderRepository = customOrderRepository;
    this.siteVisitorRepository = siteVisitorRepository;
  }

  @GetMapping
  public AdminMetricsResponse getMetrics() {
    Set<String> uniquePhones = new HashSet<>();
    uniquePhones.addAll(orderRepository.findDistinctPhoneNumbers());
    uniquePhones.addAll(customOrderRepository.findDistinctPhoneNumbers());

    BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
    Long totalVisits = siteVisitorRepository.sumVisitCount();

    return new AdminMetricsResponse(
        orderRepository.count(),
        customOrderRepository.count(),
        totalRevenue == null ? BigDecimal.ZERO : totalRevenue,
        uniquePhones.size(),
        siteVisitorRepository.count(),
        totalVisits == null ? 0L : totalVisits);
  }

  public record AdminMetricsResponse(
      Long totalOrders,
      Long totalCustomOrders,
      BigDecimal totalRevenueInr,
      Integer uniqueOrderingCustomers,
      Long uniqueVisitors,
      Long totalVisits) {
  }
}