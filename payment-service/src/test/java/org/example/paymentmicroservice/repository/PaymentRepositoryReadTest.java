package org.example.paymentmicroservice.repository;

import org.example.paymentmicroservice.model.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryReadTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void findsNewestPaymentWhenAnOrderHasMultipleAttempts() {
        Payment firstAttempt = payment("order-123", "FAILED");
        Payment latestAttempt = payment("order-123", "COMPLETED");

        paymentRepository.saveAndFlush(firstAttempt);
        Payment savedLatest = paymentRepository.saveAndFlush(latestAttempt);

        assertThat(paymentRepository.findFirstByOrderIdOrderByIdDesc("order-123"))
                .contains(savedLatest);
    }

    private Payment payment(String orderId, String status) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(new BigDecimal("89.50"));
        payment.setCurrency("EUR");
        payment.setPaymentMethod("TEST_CARD");
        payment.setStatus(status);
        return payment;
    }
}
