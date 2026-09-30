package com.yunus.service.impl;

import com.yunus.dto.payment.PaymentRequest;
import com.yunus.dto.payment.PaymentResponse;
import com.yunus.entity.Order;
import com.yunus.entity.Payment;
import com.yunus.enums.OrderStatus;
import com.yunus.enums.PaymentStatus;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.PaymentMapper;
import com.yunus.repository.PaymentRepository;
import com.yunus.service.OrderService;
import com.yunus.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderService orderService;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest paymentRequest) {
        paymentRepository.findByOrderId(paymentRequest.getOrderId()).ifPresent(payment -> {
            throw new BusinessException(ErrorType.DUPLICATE_ENTRY, "Bu siparişe ait ödeme zaten var, orderId: " + paymentRequest.getOrderId() + " ");
        });

        Order order = orderService.getOrderEntityById(paymentRequest.getOrderId());

        if (order.getTotalPrice().compareTo(paymentRequest.getAmount()) != 0) {
            throw new BusinessException(
                    ErrorType.INVALID_PAYMENT, "Beklenen : " + order.getTotalPrice() + ", gelen :  " + paymentRequest.getAmount()
            );
        }
        Payment payment = Payment.builder()
                .order(order)
                .amount(paymentRequest.getAmount())
                .method(paymentRequest.getMethod())
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .build();


        Payment savedPayment = paymentRepository.save(payment);

        orderService.updateOrderStatus(paymentRequest.getOrderId(), OrderStatus.PAID);

        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Bulunamadı, Ödeme Kaydı : " + orderId + " "));

        return paymentMapper.toResponse(payment);
    }
}
