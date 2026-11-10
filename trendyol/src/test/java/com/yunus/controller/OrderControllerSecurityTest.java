package com.yunus.controller;


import com.yunus.config.RestAccessDeniedHandler;
import com.yunus.config.RestAuthenticationEntryPoint;
import com.yunus.config.SecurityConfig;
import com.yunus.entity.User;
import com.yunus.enums.Role;
import com.yunus.model.UserPrincipal;
import com.yunus.service.CustomUserDetailsService;
import com.yunus.service.JwtService;
import com.yunus.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("OrderController Güvenliği")
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class})
public class OrderControllerSecurityTest {

    private static final long KULLANICI_ID = 1L;
    private static final long ADMIN_ID = 2L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    CustomUserDetailsService custemUserDetailsService;

    private  final UserPrincipal kullanici = principal(KULLANICI_ID, Role.USER);
    private  final UserPrincipal admin = principal(ADMIN_ID, Role.ADMIN);


    private static UserPrincipal principal(long id, Role role) {
        return new UserPrincipal(User.builder()
                .id(id)
                .username("kullanici-" + id)
                .role(role)
                .build());
    }

}
