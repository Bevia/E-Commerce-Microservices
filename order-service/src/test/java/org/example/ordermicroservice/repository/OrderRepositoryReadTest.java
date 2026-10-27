package org.example.ordermicroservice.repository;

import jakarta.persistence.EntityManagerFactory;
import org.example.ordermicroservice.dto.OrderResponse;
import org.example.ordermicroservice.model.Order;
import org.example.ordermicroservice.model.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryReadTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void orderReadQueriesLoadItemsAndCanBeMappedAfterDetaching() {
        Order order = new Order();
        order.setUserId(42L);
        order.setUsername("integration-user");
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setTotalAmount(new BigDecimal("89.50"));

        OrderItem item = new OrderItem();
        item.setProductId("SKU-1");
        item.setProductName("Test product");
        item.setQuantity(1);
        item.setPriceAtTimeOfPurchase(new BigDecimal("89.50"));
        order.addOrderItem(item);

        Long orderId = orderRepository.saveAndFlush(order).getId();
        entityManager.clear();

        Order loadedById = orderRepository.findById(orderId).orElseThrow();
        assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(loadedById, "orderItems")).isTrue();
        entityManager.detach(loadedById);

        OrderResponse response = OrderResponse.from(loadedById);
        assertThat(response.id()).isEqualTo(orderId);
        assertThat(response.orderItems()).hasSize(1);
        assertThat(response.orderItems().get(0).productId()).isEqualTo("SKU-1");

        entityManager.clear();
        assertThat(orderRepository.findAll()).allSatisfy(found ->
                assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(found, "orderItems")).isTrue());

        entityManager.clear();
        assertThat(orderRepository.findByUserId(42L)).allSatisfy(found ->
                assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(found, "orderItems")).isTrue());
    }
}
