package com.yunus.mapper;

import com.yunus.dto.payment.PaymentResponse;
import com.yunus.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(source = "order.id", target = "orderId")
    PaymentResponse toResponse(Payment payment);
}
