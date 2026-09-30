package com.yunus.service.impl;

import com.yunus.dto.order.OrderItemRequest;
import com.yunus.dto.order.OrderRequest;
import com.yunus.dto.order.OrderResponse;
import com.yunus.entity.Order;
import com.yunus.entity.OrderItem;
import com.yunus.entity.Product;
import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.OrderMapper;
import com.yunus.repository.OrderRepository;
import com.yunus.repository.ProductRepository;
import com.yunus.repository.UserRepository;
import com.yunus.service.OrderService;
import com.yunus.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final ProductService productService;


    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(
                        ErrorType.NOT_FOUND, "Kullanıcı Bulunamadı, id : " + request.getUserId()));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {


            Product product = productService.decreaseStockAndGetProduct(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();


            order.getOrderItems().add(orderItem);

            total = total.add(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotalPrice(total);

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        return null;
    }

    @Override
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return List.of();
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        return null;
    }

    @Override
    public Order getOrderEntityById(Long id) {
        return null;
    }
}
