package com.yunus.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {


    private String token;
    private String username;
    private String message;


}
