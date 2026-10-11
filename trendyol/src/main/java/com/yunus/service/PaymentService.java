package com.yunus.service;

import com.yunus.dto.payment.PaymentRequest;
import com.yunus.dto.payment.PaymentResponse;
import com.yunus.model.UserPrincipal;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest paymentRequest, UserPrincipal userPrincipal);

    PaymentResponse getPaymentByOrderId(Long orderId,UserPrincipal userPrincipal);
}
