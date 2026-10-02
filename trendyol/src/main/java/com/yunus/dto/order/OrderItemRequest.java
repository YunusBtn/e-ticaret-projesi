package com.yunus.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {
    @NotNull(message = "Ürün id boş olamaz")
    private Long productId;

    @NotNull(message = "Adet boş olamaz")
    @Positive(message = "Adet 0'dan büyük olmalıdır")
    // Positive: 0 ve negatif ASLA kabul edilmez — "0 adet ürün sipariş et"
    // anlamsız bir istek, baştan reddediyoruz
    private Integer quantity;
}
