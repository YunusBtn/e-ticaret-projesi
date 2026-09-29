package com.yunus.dto.user;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String email;
    private String password;
}
