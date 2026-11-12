package com.yunus.controller;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=YnUteWFsbml6Y2EtdGVzdC1pY2luLXVyZXRpbG1pcy1hbmFodGFyLTAxMjM0NTY3ODktYWJjZGVm",
        "jwt.expiration=900000"
})
@DisplayName("JwtFilter (gerçek JWT ile)")
public class OrderControllerJwtTest {
}
