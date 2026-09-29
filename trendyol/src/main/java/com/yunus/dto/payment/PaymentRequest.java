package com.yunus.dto.payment;

import com.yunus.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequest {
    private Long orderId;
    private BigDecimal amount;
    private PaymentMethod method;
}