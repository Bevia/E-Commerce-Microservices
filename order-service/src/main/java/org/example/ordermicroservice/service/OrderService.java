package org.example.ordermicroservice.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ordermicroservice.dto.OrderResponse;
import org.example.ordermicroservice.model.Order;
import org.example.ordermicroservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient; // For calling other microservices

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            WebClient.Builder webClientBuilder,
            @Value("${payment.service.base-url:http://localhost:8081}") String paymentServiceBaseUrl
    ) {
        this.orderRepository = orderRepository;
        this.webClient = webClientBuilder.baseUrl(paymentServiceBaseUrl).build();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public Order createOrder(Order order) {
        // 1. Calculate total amount
        BigDecimal totalAmount = order.getOrderItems().stream()
                .map(item -> item.getPriceAtTimeOfPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // 2. Set initial status and date
        order.setStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());

        // 3. Save the order to get an ID before calling payment service
        Order savedOrder = orderRepository.save(order);

        // 4. Call Payment Service to process payment
        //    (In a real app, you'd send payment method details, not just total amount)
        PaymentRequest paymentRequest = new PaymentRequest(
                savedOrder.getId().toString(), // Use orderId as reference
                savedOrder.getTotalAmount(),
                "EUR", // Or currency from order
                "Credit Card" // Placeholder
        );

        // This is where inter-service communication happens
        // Mono<PaymentResponse> is reactive, meaning it won't block the thread
        Mono<PaymentResponse> paymentResponseMono = webClient.post()
                .uri("/api/payments/process") // Endpoint of your Payment Service
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(paymentRequest)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .doOnError(e -> System.err.println("Error processing payment: " + e.getMessage())); // Log error
        // You could add .retry(3) or .onErrorReturn(...) for robustness

        // Block to get the result (for simplicity in this example, but in real apps
        // you might prefer non-blocking or event-driven updates)
        PaymentResponse paymentResponse = paymentResponseMono.block(); // BLOCKING CALL! Be careful in production

        // 5. Update order status based on payment response
        if (paymentResponse != null && "COMPLETED".equals(paymentResponse.getStatus())) {
            savedOrder.setStatus("PAID");
            savedOrder.setPaymentId(paymentResponse.getId()); // Store the payment ID from Payment Service
        } else {
            savedOrder.setStatus("PAYMENT_FAILED");
            // Handle failed payment: release stock, notify user, etc.
        }

        return orderRepository.save(savedOrder); // Save updated status
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setStatus(newStatus);
                    return OrderResponse.from(orderRepository.save(order));
                })
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public boolean orderExists(Long id) {
        return orderRepository.existsById(id);
    }

    @Transactional
    public OrderResponse saveOrder(Order order) {
        return OrderResponse.from(orderRepository.save(order));
    }

    // --- DTOs for Payment Service Communication ---
    // (These can be internal classes or separate DTOs in a shared library if common)

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PaymentRequest {
        private String orderId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PaymentResponse {
        private Long id;
        private String orderId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String status;
        private LocalDateTime paymentDate;
        private String transactionId;
    }
}
