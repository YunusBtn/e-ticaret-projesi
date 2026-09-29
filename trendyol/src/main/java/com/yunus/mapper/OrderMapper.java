package com.yunus.mapper;

import com.yunus.dto.order.OrderItemResponse;
import com.yunus.dto.order.OrderResponse;
import com.yunus.entity.Order;
import com.yunus.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "orderItems", target = "items")
    @Mapping(source = "user.username", target = "username")
    OrderResponse toResponse(Order order);


    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    OrderItemResponse toItemResponse(OrderItem orderItem);


}
