package com.yunus.service.impl;

import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;
import com.yunus.enums.Role;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.UserMapper;
import com.yunus.repository.NotificationRepository;
import com.yunus.repository.OrderRepository;
import com.yunus.repository.UserRepository;
import com.yunus.service.RefreshTokenService;
import com.yunus.service.UserServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    private static final Long HEDEF_ID = 5L;
    private static final Long VARIS_ID = 6L;
    private static final Long DIGER_ADMIN_ID = 7L;

    private static final Set<OrderStatus> AKTIF_DURUMLAR =
            Set.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.SHIPPED);

    private static final String ESKI_SIFRE_HASH = "eski-hash";
    private static final String YENI_SIFRE_HASH = "yeni-rastgele-hash";

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    RefreshTokenService refreshTokenService;

    @Captor
    ArgumentCaptor<String> sifreCaptor;

    @InjectMocks
    UserServiceImpl userService;

    private static User kullanici(Long id, Role rol) {
        return User.builder()
                .id(id)
                .username("kullanici-" + id)
                .email("kullanici-" + id + "@test.com")
                .password(ESKI_SIFRE_HASH)
                .role(rol)
                .build();
    }

    private static void degismediDogrula(User u, Role beklenenRol) {
        assertThat(u.getUsername()).isEqualTo("kullanici-" + u.getId());
        assertThat(u.getEmail()).isEqualTo("kullanici-" + u.getId() + "@test.com");
        assertThat(u.getPassword()).isEqualTo(ESKI_SIFRE_HASH);
        assertThat(u.getRole()).isEqualTo(beklenenRol);
        assertThat(u.getDeletedAt()).isNull();
    }

    private static void anonimlestiDogrula(User u) {
        assertThat(u.getUsername()).startsWith("deleted-");
        String token = u.getUsername().substring("deleted-".length());
        assertThat(u.getEmail()).isEqualTo("deleted-" + token + "@deleted.invalid");
        UUID.fromString(token);
        assertThat(u.getPassword()).isEqualTo(YENI_SIFRE_HASH);
        assertThat(u.getRole()).isEqualTo(Role.USER);
        assertThat(u.getDeletedAt()).isNotNull();
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("1) kullanıcı yok veya zaten silinmiş → 404, hiçbir yan etki yok")
        void kullaniciYok_notFound() {
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.empty());

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, null));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);

            verifyNoInteractions(orderRepository, notificationRepository, refreshTokenService, passwordEncoder);
        }

        @Test
        @DisplayName("2) aktif siparişi olan kullanıcı silinemez → 409 USER_HAS_ACTIVE_ORDERS")
        void aktifSiparisVar_conflict() {
            User hedef = kullanici(HEDEF_ID, Role.USER);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(true);

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, null));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.USER_HAS_ACTIVE_ORDERS);

            degismediDogrula(hedef, Role.USER);
            verifyNoInteractions(notificationRepository, refreshTokenService, passwordEncoder);
        }

        @Test
        @DisplayName("3) son admin + aktif sipariş → önce sipariş hatası; admin kilit sorgusu hiç çalışmaz")
        void aktifSiparis_sonAdminKontrolundenOnce() {
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(true);

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, null));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.USER_HAS_ACTIVE_ORDERS);

            verify(userRepository, never()).findAllByRoleForUpdate(any());
            degismediDogrula(hedef, Role.ADMIN);
        }

        @Test
        @DisplayName("4) sıradan kullanıcı: anonimleşir, bildirimler silinir, tokenlar iptal edilir, admin sorgusu yok")
        void siradanKullanici_anonimlesir() {
            User hedef = kullanici(HEDEF_ID, Role.USER);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn(YENI_SIFRE_HASH);

            LocalDateTime once = LocalDateTime.now();
            userService.deleteUser(HEDEF_ID, null);
            LocalDateTime sonra = LocalDateTime.now();

            anonimlestiDogrula(hedef);
            assertThat(hedef.getDeletedAt()).isBetween(once, sonra);

            verify(passwordEncoder).encode(sifreCaptor.capture());
            assertThat(sifreCaptor.getValue()).isNotEqualTo(ESKI_SIFRE_HASH);

            verify(notificationRepository).deleteByUserId(HEDEF_ID);
            verify(refreshTokenService).revokeAllForUser(HEDEF_ID);

            verify(userRepository, never()).findAllByRoleForUpdate(any());
            verify(userRepository).findByIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("5) admin ama başka admin de var: successorId gerekmez, silinir")
        void adminAmaBaskaAdminVar_silinir() {
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            User digerAdmin = kullanici(DIGER_ADMIN_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef, digerAdmin));
            when(passwordEncoder.encode(anyString())).thenReturn(YENI_SIFRE_HASH);

            userService.deleteUser(HEDEF_ID, null);

            anonimlestiDogrula(hedef);
            assertThat(digerAdmin.getRole()).isEqualTo(Role.ADMIN);
            verify(userRepository).findByIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("6) son admin + successorId yok → 409 LAST_ADMIN_PROTECTED, hiçbir şey değişmez")
        void sonAdmin_successorYok_conflict() {
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, null));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.LAST_ADMIN_PROTECTED);

            degismediDogrula(hedef, Role.ADMIN);
            verifyNoInteractions(notificationRepository, refreshTokenService, passwordEncoder);
        }

        @Test
        @DisplayName("7) son admin + successorId kendisi → 400 VALIDATION_ERROR, hiçbir şey değişmez")
        void sonAdmin_successorKendisi_validationError() {
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, HEDEF_ID));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.VALIDATION_ERROR);

            degismediDogrula(hedef, Role.ADMIN);
            verifyNoInteractions(notificationRepository, refreshTokenService, passwordEncoder);
            verify(userRepository).findByIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("8) son admin + devralan bulunamadı → 404, hedef değişmez")
        void sonAdmin_successorBulunamadi_notFound() {
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(userRepository.findByIdAndDeletedAtIsNull(VARIS_ID)).thenReturn(Optional.empty());
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, VARIS_ID));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);

            degismediDogrula(hedef, Role.ADMIN);
            verifyNoInteractions(notificationRepository, refreshTokenService, passwordEncoder);
        }

        @Test
        @DisplayName("9) son admin + geçerli devralan → devralan ADMIN olur, hedef anonimleşir")
        void sonAdmin_gecerliSuccessor_devredilir() {
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            User devralan = kullanici(VARIS_ID, Role.USER);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(userRepository.findByIdAndDeletedAtIsNull(VARIS_ID)).thenReturn(Optional.of(devralan));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef));
            when(passwordEncoder.encode(anyString())).thenReturn(YENI_SIFRE_HASH);

            userService.deleteUser(HEDEF_ID, VARIS_ID);

            assertThat(devralan.getRole()).isEqualTo(Role.ADMIN);
            anonimlestiDogrula(hedef);
            verify(notificationRepository).deleteByUserId(HEDEF_ID);
            verify(refreshTokenService).revokeAllForUser(HEDEF_ID);
        }
    }
}