package com.yunus.dto.product;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(max = 150, message = "Ürün adı en fazla 150 karakter olabilir")
    private String name;

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir")
    private String description; // zorunlu değil, bu yüzden @NotBlank yok, sadece uzunluk sınırı var

    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.0", inclusive = false, message = "Fiyat 0'dan büyük olmalıdır")
    // inclusive = false: 0.0'ın KENDİSİ geçersiz, 0'dan büyük olmak ZORUNDA —
    // bedava ürün gibi anlamsız bir durumu baştan engelliyoruz
    private BigDecimal price;

    @NotNull(message = "Stok miktarı boş olamaz")
    @PositiveOrZero(message = "Stok miktarı negatif olamaz")
    // PositiveOrZero: 0 veya üzeri kabul eder (yeni eklenen bir ürünün stoğu
    // 0 olabilir, henüz tedarik edilmemiş olabilir — ama negatif ASLA anlamlı değil)
    private Integer stockQuantity;

}
