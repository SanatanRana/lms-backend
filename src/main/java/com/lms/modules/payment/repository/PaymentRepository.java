package com.lms.modules.payment.repository;

import com.lms.modules.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByUserId(Long userId);
    List<PaymentEntity> findByCourseId(Long courseId);
    Optional<PaymentEntity> findByGatewayOrderId(String gatewayOrderId);
    Optional<PaymentEntity> findByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM PaymentEntity p WHERE p.paymentStatus = com.lms.common.enums.PaymentStatus.SUCCESS")
    Double sumSuccessfulPayments();

    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM PaymentEntity p WHERE p.paymentStatus = com.lms.common.enums.PaymentStatus.SUCCESS AND p.createdAt >= :startDate")
    Double sumSuccessfulPaymentsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM PaymentEntity p WHERE p.paymentStatus = com.lms.common.enums.PaymentStatus.SUCCESS AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    Double sumSuccessfulPaymentsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
