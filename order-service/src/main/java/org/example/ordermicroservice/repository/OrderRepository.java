package org.example.ordermicroservice.repository;


import org.example.ordermicroservice.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Override
    @EntityGraph(attributePaths = "orderItems")
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByUserId(Long userId);
}
