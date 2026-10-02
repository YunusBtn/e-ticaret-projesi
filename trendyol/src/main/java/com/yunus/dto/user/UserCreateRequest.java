package com.yunus.dto.user;

import com.yunus.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 2, max = 50, message = "Kullanıcı Adı 2 ve 50 karakter arasında olmalıdır.")
    private String username;

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @NotBlank(message = "e-posta kısmı boş olamaz")
    private String email;

    @NotBlank(message = "password kısmı boş olamaz")
    @Size(min = 6, message = "Şifre en az 6 karakterli olmalıdır.")
    private String password;

    private Role role;
}
