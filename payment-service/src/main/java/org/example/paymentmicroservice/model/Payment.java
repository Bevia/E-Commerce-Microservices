package org.example.paymentmicroservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data; // From Lombok dependency
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Generates no-arg constructor
@AllArgsConstructor // Generates constructor with all fields
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId; // Reference to an order in another service
    private BigDecimal amount;
    private String currency;
    private String paymentMethod; // e.g., "Credit Card", "PayPal"
    private String status; // e.g., "PENDING", "COMPLETED", "FAILED"
    private LocalDateTime paymentDate;
    private String transactionId; // Unique ID from payment gateway

    // You can add more fields as needed, e.g., userId, cardDetails, etc.
}