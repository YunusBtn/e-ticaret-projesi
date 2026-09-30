package com.yunus.service;

import com.yunus.dto.order.OrderRequest;
import com.yunus.dto.order.OrderResponse;
import com.yunus.entity.Order;
import com.yunus.enums.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getOrdersByUser(Long userId);

    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus);

    Order getOrderEntityById(Long id);

}
