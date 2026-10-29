package com.yunus.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    @Size(min = 10, max = 200, message = "Refresh token geçerli bir değer olmalıdır")
    private String refreshToken;

}
