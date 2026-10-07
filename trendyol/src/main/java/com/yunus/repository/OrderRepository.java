package com.yunus.repository;

import com.yunus.entity.Order;
import com.yunus.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {


    @EntityGraph(attributePaths = "user")
    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Order> findAll(Pageable pageable);
}
