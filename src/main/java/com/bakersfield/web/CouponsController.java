package com.bakersfield.web;

import com.bakersfield.service.CouponService;
import com.bakersfield.service.SaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class CouponsController {
  private final CouponService couponService;
  private final SaleService saleService;

  public CouponsController(CouponService couponService, SaleService saleService) {
    this.couponService = couponService;
    this.saleService = saleService;
  }

  @PostMapping("/validate")
  public CouponValidationResponse validate(@Valid @RequestBody CouponValidationRequest request) {
    var saleResult = saleService.applySale(request.subtotalAmountInr());
    var result = couponService.validateCoupon(
        request.code(),
        request.subtotalAmountInr(),
        request.phoneNumber());
    BigDecimal finalAmount = request.subtotalAmountInr()
        .subtract(saleResult.discountAmountInr())
        .subtract(result.discountAmountInr())
        .max(BigDecimal.ZERO);
    return new CouponValidationResponse(
        result.code(),
        result.discountAmountInr(),
        finalAmount,
        saleResult.discountAmountInr(),
        saleResult.sale() != null ? saleResult.sale().getName() : null);
  }

  public record CouponValidationRequest(
      @NotBlank String code,
      @NotNull BigDecimal subtotalAmountInr,
      @NotBlank String phoneNumber) {
  }

  public record CouponValidationResponse(
      String code,
      BigDecimal discountAmountInr,
      BigDecimal finalAmountInr,
      BigDecimal saleDiscountAmountInr,
      String saleName) {
  }
}
