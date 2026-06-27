package com.lms.modules.payment.repository;

import com.lms.modules.payment.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, Long> {
    Optional<CouponEntity> findByCode(String code);
    Optional<CouponEntity> findByCodeAndActiveTrue(String code);
}
