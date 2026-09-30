package com.yunus.service;

import com.yunus.dto.payment.PaymentRequest;
import com.yunus.dto.payment.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest paymentRequest);

    PaymentResponse getPaymentByOrderId(Long orderId);
}
