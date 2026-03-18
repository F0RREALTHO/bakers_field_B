package com.bakersfield.repository;

import com.bakersfield.model.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
  long countByCouponCodeIgnoreCase(String couponCode);

  long countByCouponCodeIgnoreCaseAndPhoneNumber(String couponCode, String phoneNumber);
}
