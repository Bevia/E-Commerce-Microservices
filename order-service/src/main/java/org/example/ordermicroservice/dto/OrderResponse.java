package org.example.ordermicroservice.dto;

import org.example.ordermicroservice.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String username,
        LocalDateTime orderDate,
        String status,
        BigDecimal totalAmount,
        Long paymentId,
        List<OrderItemResponse> orderItems
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getUsername(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPaymentId(),
                items
        );
    }
}
