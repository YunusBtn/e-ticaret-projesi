package com.yunus.controller;

import com.yunus.config.RestAccessDeniedHandler;
import com.yunus.config.RestAuthenticationEntryPoint;
import com.yunus.config.SecurityConfig;
import com.yunus.dto.common.PageResponse;
import com.yunus.dto.order.OrderResponse;
import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;
import com.yunus.enums.Role;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.model.UserPrincipal;
import com.yunus.service.CustomUserDetailsService;
import com.yunus.service.JwtService;
import com.yunus.service.OrderService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class})
@DisplayName("OrderController HTTP sözleşmesi")
class OrderControllerHttpTest {

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
        return new UserPrincipal(User.builder().id(id).username("kullanici-" + id).role(rol).build());
    }

    private ResultActions siparisGonder(String json) throws Exception {
        return mockMvc.perform(post("/api/orders/create")
                .with(user(kullanici))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    @DisplayName("items boş liste → 400, hata 'items' alanında")
    void create_bosListe_400() throws Exception {
        siparisGonder("""
                {"items": []}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.items").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("items hiç yok (null) → 400, hata 'items' alanında")
    void create_itemsYok_400() throws Exception {
        siparisGonder("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("adet 0 → 400, hata 'items[0].quantity' alanında (@Positive)")
    void create_adetSifir_400() throws Exception {
        siparisGonder("""
                {"items": [{"productId": 1, "quantity": 0}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("öğede productId ve quantity yok → 400, iki alanda da hata (@NotNull)")
    void create_urunIdVeAdetYok_400() throws Exception {
        siparisGonder("""
                {"items": [{}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['items[0].productId']").exists())
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("bozuk JSON → 400 (HttpMessageNotReadableException → handleMalformedRequest)")
    void create_bozukJson_400() throws Exception {
        siparisGonder("{\"items\": [")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("gövde JSON değil (text/plain) → 415 (HttpMediaTypeNotSupportedException)")
    void create_yanlisIcerikTipi_415() throws Exception {
        mockMvc.perform(post("/api/orders/create")
                        .with(user(kullanici))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("abc"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));

        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "yanlış metot: {0} {1} → 405")
    @CsvSource({
            "GET,    /api/orders/create",
            "POST,   /api/orders/my",
            "DELETE, /api/orders/get/10",
            "PUT,    /api/orders/10/cancel"
    })
    void yanlisHttpMetodu_405(String metot, String adres) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metot), adres).with(user(kullanici)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("yoldaki id sayı değil (/get/abc) → 400, hangi parametre olduğu mesajda")
    void yoldakiIdSayiDegil_400() throws Exception {
        mockMvc.perform(get("/api/orders/get/abc").with(user(kullanici)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Geçersiz parametre değeri : id"));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("ADMIN geçersiz newStatus gönderir (ABC) → 400")
    void gecersizDurum_400() throws Exception {
        mockMvc.perform(patch("/api/orders/10/status").param("newStatus", "ABC").with(user(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Geçersiz parametre değeri : newStatus"));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("ADMIN newStatus'u hiç göndermez → 400 (MissingServletRequestParameterException)")
    void eksikParametre_400() throws Exception {
        mockMvc.perform(patch("/api/orders/10/status").with(user(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("ÖĞRETİCİ: USER admin ucuna GEÇERSİZ parametreyle gider → 403 değil 400 (parametre çözümlemesi yetkiden önce)")
    void userGecersizParametre_403Degil400() throws Exception {
        mockMvc.perform(patch("/api/orders/10/status").param("newStatus", "ABC").with(user(kullanici)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "BusinessException({0}) → HTTP durumu enum'daki durumla aynı")
    @EnumSource(ErrorType.class)
    void businessException_enumdakiDurumaCevrilir(ErrorType tip) throws Exception {
        when(orderService.getOrderById(eq(10L), any(UserPrincipal.class)))
                .thenThrow(new BusinessException(tip, "test ayrıntısı"));

        int beklenenDurum = tip.getStatus().value();

        mockMvc.perform(get("/api/orders/get/10").with(user(kullanici)))
                .andExpect(status().is(beklenenDurum))
                .andExpect(jsonPath("$.status").value(beklenenDurum))
                .andExpect(jsonPath("$.path").value("/api/orders/get/10"));
    }

    @Test
    @DisplayName("eşzamanlı güncelleme çakışması (@Version) → ErrorType.CONCURRENT_UPDATE durumu")
    void iptalCakismasi_concurrentUpdate() throws Exception {
        when(orderService.cancelOrder(eq(10L), any(UserPrincipal.class)))
                .thenThrow(new OptimisticLockingFailureException("çakışma"));

        int beklenenDurum = ErrorType.CONCURRENT_UPDATE.getStatus().value();
        mockMvc.perform(patch("/api/orders/10/cancel").with(user(kullanici)))
                .andExpect(status().is(beklenenDurum))
                .andExpect(jsonPath("$.status").value(beklenenDurum));
    }

    @Test
    @DisplayName("beklenmeyen hata → 500 ve iç detay istemciye SIZMAZ")
    void beklenmeyenHata_500_detaySizmaz() throws Exception {
        when(orderService.getOrderById(eq(10L), any(UserPrincipal.class)))
                .thenThrow(new IllegalStateException("veritabanı bağlantı bilgisi: gizli"));

        String govde = mockMvc.perform(get("/api/orders/get/10").with(user(kullanici)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ErrorType.INTERNAL_ERROR.getMessage()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(govde).doesNotContain("gizli");
    }

    @Test
    @DisplayName("GET /my parametresiz: sayfa 0, boyut 10, createdAt azalan (@PageableDefault)")
    void my_varsayilanSayfalama() throws Exception {
        mockMvc.perform(get("/api/orders/my").with(user(kullanici)))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getOrdersByUser(eq(KULLANICI_ID), pageableCaptor.capture());
        Pageable sayfa = pageableCaptor.getValue();

        assertThat(sayfa.getPageNumber()).isZero();
        assertThat(sayfa.getPageSize()).isEqualTo(10);
        Sort.Order siralama = sayfa.getSort().getOrderFor("createdAt");
        assertThat(siralama).isNotNull();
        assertThat(siralama.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("GET /my?page=2&size=5&sort=id,asc: istemcinin verdiği değerler uygulanır")
    void my_ozelSayfalama() throws Exception {
        mockMvc.perform(get("/api/orders/my")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "id,asc")
                        .with(user(kullanici)))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getOrdersByUser(eq(KULLANICI_ID), pageableCaptor.capture());
        Pageable sayfa = pageableCaptor.getValue();

        assertThat(sayfa.getPageNumber()).isEqualTo(2);
        assertThat(sayfa.getPageSize()).isEqualTo(5);
        Sort.Order siralama = sayfa.getSort().getOrderFor("id");
        assertThat(siralama).isNotNull();
        assertThat(siralama.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("GET /my?size=500: boyut application.properties'teki üst sınıra (50) düşürülür")
    void my_asiriBuyukBoyut_50yeSinirlanir() throws Exception {
        mockMvc.perform(get("/api/orders/my").param("size", "500").with(user(kullanici)))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getOrdersByUser(eq(KULLANICI_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("GET /all: sayfa cevabı JSON'da content/totalElements/first/last alanlarıyla döner")
    void all_sayfaCevabiSekli() throws Exception {
        OrderResponse siparis = OrderResponse.builder().id(10L).status(OrderStatus.PAID).build();
        PageResponse<OrderResponse> sayfa = PageResponse.<OrderResponse>builder()
                .content(List.of(siparis))
                .page(0).size(10).totalPages(1).totalElements(1)
                .first(true).last(true)
                .build();
        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(sayfa);

        mockMvc.perform(get("/api/orders/all").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
}