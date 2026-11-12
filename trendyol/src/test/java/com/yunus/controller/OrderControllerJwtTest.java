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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=YnUteWFsbml6Y2EtdGVzdC1pY2luLXVyZXRpbG1pcy1hbmFodGFyLTAxMjM0NTY3ODktYWJjZGVm",
        "jwt.expiration=900000"
})
@DisplayName("JwtFilter (gerçek JWT ile)")
class OrderControllerJwtTest {

    private static final String TEST_SECRET =
            "YnUteWFsbml6Y2EtdGVzdC1pY2luLXVyZXRpbG1pcy1hbmFodGFyLTAxMjM0NTY3ODktYWJjZGVm";
    private static final String BASKA_ANAHTAR =
            "YmFza2EtYmlyLXRlc3QtYW5haHRhcmktaW16YS11eXVzbWF6bGlnaS1pY2luLTk4NzY1NDMyMTA=";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    OrderService orderService;

    private final UserPrincipal kullanici = principal(1L, Role.USER);
    private final UserPrincipal admin = principal(2L, Role.ADMIN);

    private static UserPrincipal principal(Long id, Role rol) {
        return new UserPrincipal(User.builder().id(id).username("kullanici-" + id).role(rol).build());
    }

    private static SecretKey anahtar(String base64) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64));
    }

    private void reddedilir(String authorizationBasligi) throws Exception {
        mockMvc.perform(get("/api/orders/my").header(HttpHeaders.AUTHORIZATION, authorizationBasligi))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("geçerli USER token'ı: istek geçer ve controller'a filtrenin kurduğu UserPrincipal ulaşır")
    void gecerliToken_kimlikKurulur() throws Exception {
        String token = jwtService.generateToken(kullanici);
        when(customUserDetailsService.loadUserByUsername("kullanici-1")).thenReturn(kullanici);

        mockMvc.perform(get("/api/orders/get/10").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        verify(orderService).getOrderById(eq(10L), same(kullanici));
    }

    @Test
    @DisplayName("geçerli ADMIN token'ı: admin ucu 200 (yetki, token'dan değil kullanıcı kaydından geliyor)")
    void gecerliAdminToken_adminUcu200() throws Exception {
        String token = jwtService.generateToken(admin);
        when(customUserDetailsService.loadUserByUsername("kullanici-2")).thenReturn(admin);

        mockMvc.perform(get("/api/orders/all").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("geçerli USER token'ı admin ucunda 403 (kimlik var ama yetki yok)")
    void gecerliUserToken_adminUcu403() throws Exception {
        String token = jwtService.generateToken(kullanici);
        when(customUserDetailsService.loadUserByUsername("kullanici-1")).thenReturn(kullanici);

        mockMvc.perform(get("/api/orders/all").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("başlık 'Bearer ' ile başlamıyorsa token hiç okunmaz → 401")
    void bearerOnekiYok_401() throws Exception {
        String token = jwtService.generateToken(kullanici);

        reddedilir(token);

        verify(customUserDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("rastgele metin token → 401, 500 değil; kullanıcı aranmaz")
    void rastgeleMetin_401() throws Exception {
        reddedilir("Bearer bu.bir.token.degil");

        verify(customUserDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("başka bir anahtarla imzalanmış token → 401 (imza uyuşmuyor)")
    void yanlisAnahtarlaImzali_401() throws Exception {
        String sahte = Jwts.builder()
                .subject("kullanici-1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(anahtar(BASKA_ANAHTAR))
                .compact();

        reddedilir("Bearer " + sahte);

        verify(customUserDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("süresi dolmuş token → 401 (imza doğru olsa bile)")
    void suresiDolmus_401() throws Exception {
        String eski = Jwts.builder()
                .subject("kullanici-1")
                .issuedAt(new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000))
                .expiration(new Date(System.currentTimeMillis() - 60 * 60 * 1000))
                .signWith(anahtar(TEST_SECRET))
                .compact();

        reddedilir("Bearer " + eski);

        verify(customUserDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("imzasız token (alg=none saldırısı) → 401")
    void imzasizToken_401() throws Exception {
        String imzasiz = Jwts.builder()
                .subject("kullanici-2")
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .compact();

        reddedilir("Bearer " + imzasiz);

        verify(customUserDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("token geçerli ama kullanıcı artık yok (UsernameNotFoundException) → 401")
    void kullaniciBulunamadi_401() throws Exception {
        String token = jwtService.generateToken(kullanici);
        when(customUserDetailsService.loadUserByUsername("kullanici-1"))
                .thenThrow(new UsernameNotFoundException("yok"));

        reddedilir("Bearer " + token);

        verify(customUserDetailsService).loadUserByUsername("kullanici-1");
    }

    @Test
    @DisplayName("token başka bir kullanıcıya ait çıkarsa (ad uyuşmuyor) → 401")
    void tokenKullaniciAdiUyusmuyor_401() throws Exception {
        String token = jwtService.generateToken(kullanici);
        UserPrincipal baskasi = new UserPrincipal(
                User.builder().id(9L).username("baska-biri").role(Role.USER).build());
        when(customUserDetailsService.loadUserByUsername("kullanici-1")).thenReturn(baskasi);

        reddedilir("Bearer " + token);
    }
}