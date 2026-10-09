package com.yunus.service;

import com.yunus.dto.common.PageResponse;
import com.yunus.dto.order.OrderRequest;
import com.yunus.dto.order.OrderResponse;
import com.yunus.entity.Order;
import com.yunus.enums.OrderStatus;
import com.yunus.model.UserPrincipal;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse getOrderById(Long id, UserPrincipal principal);

    PageResponse<OrderResponse> getOrdersByUser(Long userId,Pageable pageable);

    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus);

    Order getOrderEntityById(Long id);

    PageResponse<OrderResponse> getAllOrders(Pageable pageable);

    OrderResponse createOrder(OrderRequest request, UserPrincipal principal);
}
