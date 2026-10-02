package com.yunus.dto.payment;

import com.yunus.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequest {
    @NotNull(message = "Sipariş id boş olamaz")
    private Long orderId;

    @NotNull(message = "Tutar boş olamaz")
    @Positive(message = "Tutar 0'dan büyük olmalıdır")
    private BigDecimal amount;

    @NotNull(message = "Ödeme yöntemi boş olamaz")
    private PaymentMethod method;
}