package com.yunus.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {


    @NotBlank(message = "Kullanıcı Adı Boş Olamaz")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arasında olmalıdır.")
    private String username;

    @NotBlank(message = "Şifre Boş Olamaz")
    @Size(min = 6, max = 50, message = "Şifre en az 6 karakter  olmalıdır.")
    private String password;

    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    @NotBlank(message = "e-posta boş olamaz")
    private String email;


}
