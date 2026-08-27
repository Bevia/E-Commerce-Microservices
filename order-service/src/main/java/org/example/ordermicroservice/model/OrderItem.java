package org.example.ordermicroservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId; // ID of the product from a (future) Product Service
    private String productName; // Denormalized for convenience
    private Integer quantity;
    @Column(precision = 19, scale = 2)
    private BigDecimal priceAtTimeOfPurchase; // Price at the moment of order creation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false) // Managed by Order entity
    private Order order; // Reference back to the Order
}
