package com.yunus.service.impl;

import com.yunus.entity.Order;
import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;
import com.yunus.enums.Role;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.OrderMapper;
import com.yunus.model.UserPrincipal;
import com.yunus.repository.OrderRepository;
import com.yunus.repository.UserRepository;
import com.yunus.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest{

    private static final Long SAHIP_ID = 1L;
    private static final Long BASKA_KULLANICI_ID = 2L;
    private static final Long ADMIN_ID = 3L;
    private static final Long SIPARIS_ID = 10L;

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderMapper orderMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    ProductService productService;

    @InjectMocks
    OrderServiceImpl orderService;

    private static User kullanici(Long id, Role rol) {
        return User.builder()
                .id(id)
                .role(rol)
                .build();
    }

    private static UserPrincipal principal(Long id, Role rol) {
        return new UserPrincipal(kullanici(id, rol));
    }

    private static Order siparis(Long id, Long sahipId, OrderStatus durum) {
        return Order.builder()
                .id(id)
                .user(kullanici(sahipId, Role.USER))
                .status(durum)
                .build();
    }

    @Nested
    @DisplayName("getOrderById()")
    class GetOrderById {

        @Test
        @DisplayName("sahibi kendi siparişini görebilir")
        void sahip_kendiSiparisiniGorur() {
            Order order = siparis(SIPARIS_ID, SAHIP_ID, OrderStatus.PENDING);
            when(orderRepository.findById(SIPARIS_ID)).thenReturn(Optional.of(order));

            orderService.getOrderById(SIPARIS_ID, principal(SAHIP_ID, Role.USER));

            verify(orderMapper).toResponse(order);
        }

        @Test
        @DisplayName("admin başkasının siparişini görebilir")
        void admin_baskasininSiparisiniGorur() {
            Order order = siparis(SIPARIS_ID, SAHIP_ID, OrderStatus.PENDING);
            when(orderRepository.findById(SIPARIS_ID)).thenReturn(Optional.of(order));

            orderService.getOrderById(SIPARIS_ID, principal(ADMIN_ID, Role.ADMIN));

            verify(orderMapper).toResponse(order);
        }

        @Test
        @DisplayName("başka kullanıcı siparişi göremez: 404 NOT_FOUND")
        void baskaKullanici_notFoundAlir() {
            when(orderRepository.findById(SIPARIS_ID))
                    .thenReturn(Optional.of(siparis(SIPARIS_ID, SAHIP_ID, OrderStatus.PENDING)));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> orderService.getOrderById(SIPARIS_ID, principal(BASKA_KULLANICI_ID, Role.USER)));

            assertThat(hata.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verifyNoInteractions(orderMapper);
        }

        @Test
        @DisplayName("olmayan sipariş: 404 NOT_FOUND")
        void olmayanSiparis_notFoundAlir() {
            when(orderRepository.findById(SIPARIS_ID)).thenReturn(Optional.empty());

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> orderService.getOrderById(SIPARIS_ID, principal(SAHIP_ID, Role.USER)));

            assertThat(hata.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("başkasının siparişi ile olmayan sipariş birebir aynı hatayı verir (enumeration koruması)")
        void baskasininSiparisi_ileOlmayanSiparis_ayniHatayiVerir() {
            when(orderRepository.findById(SIPARIS_ID))
                    .thenReturn(Optional.of(siparis(SIPARIS_ID, SAHIP_ID, OrderStatus.PENDING)))
                    .thenReturn(Optional.empty());
            UserPrincipal yabanci = principal(BASKA_KULLANICI_ID, Role.USER);

            BusinessException baskasininSiparisiHatasi = assertThrows(BusinessException.class,
                    () -> orderService.getOrderById(SIPARIS_ID, yabanci));
            BusinessException olmayanSiparisHatasi = assertThrows(BusinessException.class,
                    () -> orderService.getOrderById(SIPARIS_ID, yabanci));

            assertThat(baskasininSiparisiHatasi.getErrorType())
                    .isEqualTo(olmayanSiparisHatasi.getErrorType())
                    .isEqualTo(ErrorType.NOT_FOUND);
            assertThat(baskasininSiparisiHatasi.getMessage())
                    .isEqualTo(olmayanSiparisHatasi.getMessage());
        }
    }

    @Nested
    @DisplayName("getOrderEntityForOwner()")
    class GetOrderEntityForOwner {

        @Test
        @DisplayName("sahibi siparişin kendisini alır")
        void sahip_siparisinKendisiniAlir() {
            Order order = siparis(SIPARIS_ID, SAHIP_ID, OrderStatus.PENDING);
            when(orderRepository.findById(SIPARIS_ID)).thenReturn(Optional.of(order));

            Order sonuc = orderService.getOrderEntityForOwner(SIPARIS_ID, principal(SAHIP_ID, Role.USER));

            assertThat(sonuc).isSameAs(order);
        }

        @Test
        @DisplayName("admin bile sahibi değilse siparişi alamaz: 404 NOT_FOUND")
        void admin_sahipDegilse_notFoundAlir() {
            when(orderRepository.findById(SIPARIS_ID))
                    .thenReturn(Optional.of(siparis(SIPARIS_ID, SAHIP_ID, OrderStatus.PENDING)));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> orderService.getOrderEntityForOwner(SIPARIS_ID, principal(ADMIN_ID, Role.ADMIN)));

            assertThat(hata.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

}