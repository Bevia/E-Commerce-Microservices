package org.example.ordermicroservice.dto;

import org.example.ordermicroservice.model.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        String productId,
        String productName,
        Integer quantity,
        BigDecimal priceAtTimeOfPurchase
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPriceAtTimeOfPurchase()
        );
    }
}
