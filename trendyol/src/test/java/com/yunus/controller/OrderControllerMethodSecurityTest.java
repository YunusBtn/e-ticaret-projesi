package com.yunus.controller;

import com.yunus.config.RestAccessDeniedHandler;
import com.yunus.config.RestAuthenticationEntryPoint;
import com.yunus.config.SecurityConfig;
import com.yunus.service.CustomUserDetailsService;
import com.yunus.service.JwtService;
import com.yunus.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class})
@DisplayName("OrderController method security")
class OrderControllerMethodSecurityTest {

    @Autowired
    OrderController orderController;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("method security açık: controller bean'i yetki kontrolü yapan bir vekil (proxy)")
    void controller_yetkiVekiliOlarakCalisir() {
        assertThat(AopUtils.isAopProxy(orderController))
                .as("OrderController vekil değil → @EnableMethodSecurity kapalı olabilir, @PreAuthorize'lar işlemiyor")
                .isTrue();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER bağlamında admin metodu doğrudan çağrılırsa AccessDeniedException fırlar")
    void user_adminMetodunuDogrudanCagirirsa_reddedilir() {
        assertThrows(AccessDeniedException.class, () -> orderController.getAllOrders(Pageable.unpaged()));

        verifyNoInteractions(orderService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN bağlamında aynı metot çağrılabilir ve servise ulaşır")
    void admin_adminMetodunuDogrudanCagirirsa_gecer() {
        assertDoesNotThrow(() -> orderController.getAllOrders(Pageable.unpaged()));

        verify(orderService).getAllOrders(any(Pageable.class));
    }

    private static final Set<String> ADMIN_UCLARI =
            Set.of("getOrdersByUser", "updateOrderStatus", "getAllOrders");
    private static final Set<String> HERKES_UCLARI =
            Set.of("createOrder", "cancelOrder", "getOrderById", "getMyOrders");

    @Test
    @DisplayName("her HTTP ucunun güvenlik kararı listede var; admin uçları @PreAuthorize taşır, diğerleri taşımaz")
    void ucEnvanteri_herUcunGuvenlikKarariVar() {
        List<Method> ucMetotlari = Arrays.stream(OrderController.class.getDeclaredMethods())
                .filter(m -> AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class))
                .toList();

        Set<String> gercekAdlar = ucMetotlari.stream().map(Method::getName).collect(Collectors.toSet());
        Set<String> beklenenAdlar = new HashSet<>(ADMIN_UCLARI);
        beklenenAdlar.addAll(HERKES_UCLARI);
        assertThat(gercekAdlar)
                .as("Controller'a uç eklendi/silindi: yeni ucun yetkisini ADMIN_UCLARI ya da HERKES_UCLARI'na yaz")
                .containsExactlyInAnyOrderElementsOf(beklenenAdlar);

        for (Method uc : ucMetotlari) {
            PreAuthorize yetki = uc.getAnnotation(PreAuthorize.class);
            if (ADMIN_UCLARI.contains(uc.getName())) {
                assertThat(yetki).as(uc.getName() + " admin ucu: @PreAuthorize taşımalı").isNotNull();
                assertThat(yetki.value()).as(uc.getName() + " yetki ifadesi").isEqualTo("hasRole('ADMIN')");
            } else {
                assertThat(yetki).as(uc.getName() + " herkese açık uç: @PreAuthorize taşımamalı").isNull();
            }
        }
    }
}