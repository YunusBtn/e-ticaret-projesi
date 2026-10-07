package com.yunus.service;

import com.yunus.dto.common.PageResponse;
import com.yunus.dto.order.OrderRequest;
import com.yunus.dto.order.OrderResponse;
import com.yunus.entity.Order;
import com.yunus.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    PageResponse<OrderResponse> getOrdersByUser(Long userId,Pageable pageable);

    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus);

    Order getOrderEntityById(Long id);

    PageResponse<OrderResponse> getAllOrders(Pageable pageable);

}
