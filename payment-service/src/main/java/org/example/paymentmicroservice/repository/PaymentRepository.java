package org.example.paymentmicroservice.repository;

import org.example.paymentmicroservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // You can add custom query methods if needed, e.g.:
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByStatus(String status);
}