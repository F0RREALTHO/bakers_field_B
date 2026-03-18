package com.bakersfield.service;

import com.bakersfield.model.Coupon;
import com.bakersfield.model.CouponRedemption;
import com.bakersfield.repository.CouponRedemptionRepository;
import com.bakersfield.repository.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CouponService {
  private final CouponRepository couponRepository;
  private final CouponRedemptionRepository redemptionRepository;

  public CouponService(
      CouponRepository couponRepository,
      CouponRedemptionRepository redemptionRepository) {
    this.couponRepository = couponRepository;
    this.redemptionRepository = redemptionRepository;
  }

  public Coupon getByCode(String code) {
    return couponRepository.findByCodeIgnoreCase(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
  }

  public CouponValidationResult validateCoupon(
      String code,
      BigDecimal subtotalAmountInr,
      String phoneNumber) {
    Coupon coupon = getByCode(code);
    Instant now = Instant.now();

    if (!coupon.isActive()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is inactive");
    }
    if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon not active yet");
    }
    if (coupon.getEndsAt() != null && now.isAfter(coupon.getEndsAt())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon expired");
    }
    if (coupon.getMinOrderAmount() != null
        && subtotalAmountInr.compareTo(coupon.getMinOrderAmount()) < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order total too low");
    }

    if (coupon.getUsageLimit() != null) {
      long totalUsed = redemptionRepository.countByCouponCodeIgnoreCase(coupon.getCode());
      if (totalUsed >= coupon.getUsageLimit()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon usage limit reached");
      }
    }

    if (coupon.getPerCustomerLimit() != null && phoneNumber != null) {
      long usedByCustomer = redemptionRepository
          .countByCouponCodeIgnoreCaseAndPhoneNumber(coupon.getCode(), phoneNumber);
      if (usedByCustomer >= coupon.getPerCustomerLimit()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon already used" );
      }
    }

    BigDecimal discount = calculateDiscount(coupon, subtotalAmountInr);
    BigDecimal finalAmount = subtotalAmountInr.subtract(discount).max(BigDecimal.ZERO);

    return new CouponValidationResult(
        coupon.getCode(),
        discount.setScale(2, RoundingMode.HALF_UP),
        finalAmount.setScale(2, RoundingMode.HALF_UP));
  }

  @Transactional
  public CouponValidationResult applyCoupon(
      String code,
      BigDecimal subtotalAmountInr,
      String phoneNumber) {
    CouponValidationResult result = validateCoupon(code, subtotalAmountInr, phoneNumber);
    Coupon coupon = getByCode(code);

    CouponRedemption redemption = new CouponRedemption();
    redemption.setCouponCode(coupon.getCode());
    redemption.setPhoneNumber(phoneNumber == null ? "" : phoneNumber);
    redemptionRepository.save(redemption);

    coupon.setTimesUsed(coupon.getTimesUsed() + 1);
    couponRepository.save(coupon);

    return result;
  }

  private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotalAmountInr) {
    if (subtotalAmountInr.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    return switch (coupon.getType()) {
      case PERCENT -> subtotalAmountInr
          .multiply(coupon.getAmount())
          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
          .min(subtotalAmountInr);
      case FLAT -> coupon.getAmount().min(subtotalAmountInr);
    };
  }

  public record CouponValidationResult(
      String code,
      BigDecimal discountAmountInr,
      BigDecimal finalAmountInr) {
  }
}
