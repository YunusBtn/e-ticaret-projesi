package com.yunus.dto.user;

import com.yunus.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 50, message = "Kullanıcı Adı 3 ve 50 karakter arasında olmalıdır.")
    private String username;

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @NotBlank(message = "e-posta kısmı boş olamaz")
    private String email;

    private Role role;
}
