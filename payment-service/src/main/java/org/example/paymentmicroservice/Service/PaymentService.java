package org.example.paymentmicroservice.Service;

import org.example.paymentmicroservice.model.Payment;
import org.example.paymentmicroservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment processPayment(Payment payment) {
        // Here you would integrate with a real payment gateway (e.g., Stripe, PayPal)
        // For this example, we'll just simulate a successful payment.

        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("COMPLETED"); // Simulate success
        payment.setTransactionId("TXN_" + System.currentTimeMillis()); // Simulate a transaction ID

        return paymentRepository.save(payment);
    }

    public Payment updatePayment(Payment payment) {
        // Add validation or specific update logic here
        return paymentRepository.save(payment);
    }

    public void deletePayment(Long id) {
        paymentRepository.deleteById(id);
    }

    public Optional<Payment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId);
    }
}
