package org.example.ordermicroservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL, so use "orders"
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // ID of the user who placed the order
    private String username; // Username from JWT token
    private LocalDateTime orderDate;
    private String status; // e.g., "PENDING", "PAID", "SHIPPED", "CANCELLED"
    private BigDecimal totalAmount;
    private Long paymentId; // Reference to the Payment ID from the Payment Service

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id") // Foreign key in OrderItem table pointing to this Order's ID
    private List<OrderItem> orderItems = new ArrayList<>();

    // Helper method to add items (optional, but good practice)
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }
}