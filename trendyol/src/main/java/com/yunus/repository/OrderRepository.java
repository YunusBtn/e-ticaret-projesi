package com.yunus.repository;

import com.yunus.entity.Order;
import com.yunus.enums.OrderStatus;

import java.util.List;

public interface OrderRepository {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);


}
