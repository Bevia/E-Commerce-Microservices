package org.example.paymentmicroservice.controller;

import org.example.paymentmicroservice.Service.PaymentService;
import org.example.paymentmicroservice.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments") // Base path for all endpoints in this controller
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(payment -> new ResponseEntity<>(payment, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable String orderId) {
        return paymentService.getPaymentByOrderId(orderId)
                .map(payment -> new ResponseEntity<>(payment, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@RequestBody Payment payment) {
        // In a real scenario, you'd perform more validation and gateway integration here.
        Payment processedPayment = paymentService.processPayment(payment);
        return new ResponseEntity<>(processedPayment, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Payment> updatePayment(@PathVariable Long id, @RequestBody Payment payment) {
        return paymentService.getPaymentById(id)
                .map(existingPayment -> {
                    // Update relevant fields
                    existingPayment.setAmount(payment.getAmount());
                    existingPayment.setStatus(payment.getStatus());
                    existingPayment.setPaymentMethod(payment.getPaymentMethod());
                    // ... and other fields that are allowed to be updated

                    Payment updatedPayment = paymentService.updatePayment(existingPayment);
                    return new ResponseEntity<>(updatedPayment, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        if (paymentService.getPaymentById(id).isPresent()) {
            paymentService.deletePayment(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
