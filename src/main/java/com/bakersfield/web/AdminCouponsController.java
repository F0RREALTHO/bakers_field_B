package com.bakersfield.web;

import com.bakersfield.model.Coupon;
import com.bakersfield.repository.CouponRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/coupons")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminCouponsController {
  private final CouponRepository couponRepository;
  private final DataChangePublisher dataChangePublisher;

  public AdminCouponsController(CouponRepository couponRepository, DataChangePublisher dataChangePublisher) {
    this.couponRepository = couponRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping
  public List<CouponResponse> getAll() {
    return couponRepository.findAll().stream().map(CouponResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CouponResponse create(@Valid @RequestBody CouponRequest request) {
    String code = normalizeCode(request.code());
    if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon already exists");
    }
    Coupon coupon = new Coupon();
    applyRequest(coupon, request, code);
    CouponResponse response = CouponResponse.from(couponRepository.save(coupon));
    dataChangePublisher.publish("coupons");
    return response;
  }

  @PutMapping("/{id}")
  public CouponResponse update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
    Coupon coupon = couponRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
    String code = normalizeCode(request.code());
    couponRepository.findByCodeIgnoreCase(code)
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon already exists");
        });
    applyRequest(coupon, request, code);
    CouponResponse response = CouponResponse.from(couponRepository.save(coupon));
    dataChangePublisher.publish("coupons");
    return response;
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!couponRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found");
    }
    couponRepository.deleteById(id);
    dataChangePublisher.publish("coupons");
  }

  private void applyRequest(Coupon coupon, CouponRequest request, String code) {
    coupon.setCode(code);
    coupon.setType(request.type());
    coupon.setAmount(request.amount());
    coupon.setMinOrderAmount(request.minOrderAmount() == null ? BigDecimal.ZERO : request.minOrderAmount());
    coupon.setStartsAt(request.startsAt());
    coupon.setEndsAt(request.endsAt());
    coupon.setUsageLimit(request.usageLimit());
    coupon.setPerCustomerLimit(request.perCustomerLimit());
    coupon.setActive(request.active());
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase();
  }

  public record CouponRequest(
      @NotBlank String code,
      @NotNull Coupon.CouponType type,
      @NotNull BigDecimal amount,
      BigDecimal minOrderAmount,
      Instant startsAt,
      Instant endsAt,
      Integer usageLimit,
      Integer perCustomerLimit,
      boolean active) {
  }

  public record CouponResponse(
      Long id,
      String code,
      Coupon.CouponType type,
      BigDecimal amount,
      BigDecimal minOrderAmount,
      Instant startsAt,
      Instant endsAt,
      Integer usageLimit,
      Integer perCustomerLimit,
      Integer timesUsed,
      boolean active) {

    public static CouponResponse from(Coupon coupon) {
      return new CouponResponse(
          coupon.getId(),
          coupon.getCode(),
          coupon.getType(),
          coupon.getAmount(),
          coupon.getMinOrderAmount(),
          coupon.getStartsAt(),
          coupon.getEndsAt(),
          coupon.getUsageLimit(),
          coupon.getPerCustomerLimit(),
          coupon.getTimesUsed(),
          coupon.isActive());
    }
  }
}
