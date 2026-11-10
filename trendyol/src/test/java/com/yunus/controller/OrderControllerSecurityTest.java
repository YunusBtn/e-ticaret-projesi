package com.yunus.controller;

import com.yunus.config.RestAccessDeniedHandler;
import com.yunus.config.RestAuthenticationEntryPoint;
import com.yunus.config.SecurityConfig;
import com.yunus.dto.order.OrderRequest;
import com.yunus.dto.order.OrderResponse;
import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;
import com.yunus.enums.Role;
import com.yunus.model.UserPrincipal;
import com.yunus.service.CustomUserDetailsService;
import com.yunus.service.JwtService;
import com.yunus.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class})
@DisplayName("OrderController güvenliği")
class OrderControllerSecurityTest {

    private static final Long KULLANICI_ID = 1L;
    private static final Long ADMIN_ID = 2L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    private final UserPrincipal kullanici = principal(KULLANICI_ID, Role.USER);
    private final UserPrincipal admin = principal(ADMIN_ID, Role.ADMIN);

    private static UserPrincipal principal(Long id, Role rol) {
        return new UserPrincipal(User.builder()
                .id(id)
                .username("kullanici-" + id)
                .role(rol)
                .build());
    }

    @ParameterizedTest(name = "anonim: {0} {1} → 401")
    @CsvSource({
            "POST,   /api/orders/create",
            "PATCH,  /api/orders/10/cancel",
            "GET,    /api/orders/get/10",
            "GET,    /api/orders/my",
            "GET,    /api/orders/user/5",
            "PATCH,  /api/orders/10/status?newStatus=SHIPPED",
            "GET,    /api/orders/all"
    })
    void anonim_tumUclarda401(String metot, String adres) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metot), adres))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "USER: {0} {1} → 403")
    @CsvSource({
            "GET,    /api/orders/user/5",
            "PATCH,  /api/orders/10/status?newStatus=SHIPPED",
            "GET,    /api/orders/all"
    })
    void user_adminUclarinda403(String metot, String adres) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metot), adres).with(user(kullanici)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "ADMIN: {0} {1} → 200")
    @CsvSource({
            "GET,    /api/orders/user/5",
            "PATCH,  /api/orders/10/status?newStatus=SHIPPED",
            "GET,    /api/orders/all"
    })
    void admin_adminUclarinda200(String metot, String adres) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metot), adres).with(user(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /my: sayfa sorgusu token'daki kullanıcının id'siyle yapılır")
    void my_tokenKullanicisiIdsiyleCagrilir() throws Exception {
        mockMvc.perform(get("/api/orders/my").with(user(kullanici)))
                .andExpect(status().isOk());

        verify(orderService).getOrdersByUser(eq(KULLANICI_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /get/{id}: sipariş id'si yoldan, kullanıcı token'dan gelir; cevap JSON'a çevrilir")
    void getOrder_servisTokenKullanicisiylaCagrilir() throws Exception {
        OrderResponse cevap = OrderResponse.builder().id(10L).status(OrderStatus.PENDING).build();
        when(orderService.getOrderById(eq(10L), any(UserPrincipal.class))).thenReturn(cevap);

        mockMvc.perform(get("/api/orders/get/10").with(user(kullanici)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).getOrderById(eq(10L), same(kullanici));
    }

    @Test
    @DisplayName("PATCH /{id}/cancel: iptal, token'daki kullanıcıyla istenir")
    void cancel_servisTokenKullanicisiylaCagrilir() throws Exception {
        mockMvc.perform(patch("/api/orders/10/cancel").with(user(kullanici)))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(eq(10L), same(kullanici));
    }

    @Test
    @DisplayName("POST /create: geçerli gövde → 201, gövde nesneye çevrilir, servis token kullanıcısıyla çağrılır")
    void create_gecerliGovde_201() throws Exception {
        OrderResponse cevap = OrderResponse.builder().id(100L).status(OrderStatus.PENDING).build();
        when(orderService.createOrder(any(OrderRequest.class), any(UserPrincipal.class))).thenReturn(cevap);
        String json = """
                {"items": [{"productId": 1, "quantity": 2}]}
                """;

        mockMvc.perform(post("/api/orders/create")
                        .with(user(kullanici))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));

        ArgumentCaptor<OrderRequest> istekCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderService).createOrder(istekCaptor.capture(), same(kullanici));
        OrderRequest istek = istekCaptor.getValue();
        assertThat(istek.getItems()).hasSize(1);
        assertThat(istek.getItems().get(0).getProductId()).isEqualTo(1L);
        assertThat(istek.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("ADMIN GET /user/{userId}: yoldaki kullanıcı id'si servise iletilir")
    void admin_baskaKullaniciSiparisleri_pathIdIletilir() throws Exception {
        mockMvc.perform(get("/api/orders/user/5").with(user(admin)))
                .andExpect(status().isOk());

        verify(orderService).getOrdersByUser(eq(5L), any(Pageable.class));
    }

    @Test
    @DisplayName("ADMIN PATCH /{id}/status: newStatus metni enum'a çevrilip servise iletilir")
    void admin_durumGuncelle_enumIletilir() throws Exception {
        mockMvc.perform(patch("/api/orders/10/status").param("newStatus", "SHIPPED").with(user(admin)))
                .andExpect(status().isOk());

        verify(orderService).updateOrderStatus(10L, OrderStatus.SHIPPED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("@WithMockUser ROL kontrolü için yeterli: admin ucu 200 (principal kullanılmıyor)")
    void withMockUser_rolKontrolu_calisir() throws Exception {
        mockMvc.perform(get("/api/orders/all"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ÖĞRETİCİ: @WithMockUser ile /my → 500: principal UserPrincipal değil, null geliyor")
    void withMockUser_principalTipiUymaz_500() throws Exception {
        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isInternalServerError());
    }
}