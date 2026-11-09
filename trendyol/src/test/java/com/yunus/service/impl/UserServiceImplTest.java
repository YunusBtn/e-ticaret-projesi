package com.yunus.service.impl;
// Test edilen UserServiceImpl ile AYNI paket, ama src/TEST/java altında.

// ---------- Projenin kendi sınıfları ----------
import com.yunus.entity.User;                          // GERÇEK entity: silinecek kullanıcı
import com.yunus.enums.OrderStatus;                    // sipariş durumları (aktif küme için)
import com.yunus.enums.Role;                           // USER / ADMIN
import com.yunus.exception.BusinessException;          // servisin fırlattığı hata
import com.yunus.exception.ErrorType;                  // hata türleri
import com.yunus.mapper.UserMapper;                    // SAHTE (deleteUser kullanmıyor ama yapıcıda var)
import com.yunus.repository.NotificationRepository;    // SAHTE: bildirim tablosu
import com.yunus.repository.OrderRepository;           // SAHTE: sipariş tablosu
import com.yunus.repository.UserRepository;            // SAHTE: kullanıcı tablosu

// ---------- JUnit ----------
import com.yunus.service.RefreshTokenService;
import com.yunus.service.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// ---------- Mockito ----------
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// ---------- Spring ----------
import org.springframework.security.crypto.password.PasswordEncoder;   // SAHTE: şifre hashleyici

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

/*
 * UserServiceImpl.deleteUser için BİRİM testleri.
 *
 * deleteUser "soft delete + anonimleştirme" yapar: kayıt silinmez, kişisel veriler (kullanıcı adı, e-posta,
 * şifre) geri döndürülemez biçimde değiştirilir. Bu testler iş kurallarının SIRASINI ve SONUCUNU doğrular.
 * Veritabanına gerçekten yazılıp yazılmadığı (dirty checking) ve kilidin etkisi Bölüm C'de kanıtlanacak.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    // ==================================================================================
    // SABİTLER
    // ==================================================================================
    private static final Long HEDEF_ID = 5L;         // silinmek istenen kullanıcı
    private static final Long VARIS_ID = 6L;         // devralacak kullanıcı (successor)
    private static final Long DIGER_ADMIN_ID = 7L;   // başka bir admin

    // Servisteki ACTIVE_ORDER_STATUSES ile AYNI küme. Stub'ta argüman olarak kullanılır:
    // Set.equals eleman eşitliğine baktığı için Set.of(...) ile üretilen küme eşleşir.
    // Servis farklı bir küme (örn. DELIVERED dahil) gönderirse stub eşleşmez ve test kırılır.
    private static final Set<OrderStatus> AKTIF_DURUMLAR =
            Set.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.SHIPPED);

    private static final String ESKI_SIFRE_HASH = "eski-hash";
    private static final String YENI_SIFRE_HASH = "yeni-rastgele-hash";

    // ==================================================================================
    // SAHTE BAĞIMLILIKLAR
    // UserServiceImpl'in yapıcısı: (UserRepository, UserMapper, PasswordEncoder,
    //                               NotificationRepository, OrderRepository, RefreshTokenService)
    // ==================================================================================
    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;   // deleteUser kullanmıyor; @InjectMocks yapıcıyı doldursun diye var

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    RefreshTokenService refreshTokenService;

    @Captor
    ArgumentCaptor<String> sifreCaptor;   // passwordEncoder.encode'a giden ham şifreyi yakalamak için

    @InjectMocks
    UserServiceImpl userService;          // GERÇEK servis

    // ==================================================================================
    // TEST VERİSİ YARDIMCILARI
    // ==================================================================================

    // Bilinen alanlarla bir kullanıcı üretir. Alan değerleri id'ye bağlı, böylece "değişmedi" kontrolü kolay.
    private static User kullanici(Long id, Role rol) {
        return User.builder()
                .id(id)
                .username("kullanici-" + id)
                .email("kullanici-" + id + "@test.com")
                .password(ESKI_SIFRE_HASH)
                .role(rol)
                .build();   // deletedAt verilmedi → null: kullanıcı aktif
    }

    // Hata sonrası kullanıcının HİÇBİR alanı değişmemiş olmalı.
    private static void degismediDogrula(User u, Role beklenenRol) {
        assertThat(u.getUsername()).isEqualTo("kullanici-" + u.getId());
        assertThat(u.getEmail()).isEqualTo("kullanici-" + u.getId() + "@test.com");
        assertThat(u.getPassword()).isEqualTo(ESKI_SIFRE_HASH);
        assertThat(u.getRole()).isEqualTo(beklenenRol);
        assertThat(u.getDeletedAt()).isNull();
    }

    // Başarıdan sonra kullanıcı anonimleşmiş olmalı (zaman kontrolü ayrıca, 4. testte).
    private static void anonimlestiDogrula(User u) {
        assertThat(u.getUsername()).startsWith("deleted-");
        // Kullanıcı adı ve e-posta AYNI rastgele token'ı paylaşıyor: "deleted-<token>" / "deleted-<token>@deleted.invalid"
        String token = u.getUsername().substring("deleted-".length());
        assertThat(u.getEmail()).isEqualTo("deleted-" + token + "@deleted.invalid");
        // Token gerçekten bir UUID mi? Değilse fromString IllegalArgumentException fırlatır ve test kırılır.
        UUID.fromString(token);
        assertThat(u.getPassword()).isEqualTo(YENI_SIFRE_HASH);   // eski hash gitti
        assertThat(u.getRole()).isEqualTo(Role.USER);             // yetki düşürüldü
        assertThat(u.getDeletedAt()).isNotNull();                 // soft delete işareti
    }

    // ==================================================================================
    // deleteUser()
    // ==================================================================================
    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("1) kullanıcı yok veya zaten silinmiş → 404, hiçbir yan etki yok")
        void kullaniciYok_notFound() {
            // ARRANGE: aktif kullanıcı bulunamıyor
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.empty());

            // ACT + ASSERT
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, null));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);

            // Sipariş, bildirim, token ve şifre servislerine hiç dokunulmadı
            verifyNoInteractions(orderRepository, notificationRepository, refreshTokenService, passwordEncoder);
        }

        @Test
        @DisplayName("2) aktif siparişi olan kullanıcı silinemez → 409 USER_HAS_ACTIVE_ORDERS")
        void aktifSiparisVar_conflict() {
            // ARRANGE: kullanıcı var ve PENDING/PAID/SHIPPED durumunda siparişi var.
            // Stub yalnızca TAM bu küme ile eşleşir: servis kümeyi değiştirirse true dönmez, test kırılır.
            User hedef = kullanici(HEDEF_ID, Role.USER);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(true);

            // ACT + ASSERT
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, null));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.USER_HAS_ACTIVE_ORDERS);

            // Hiçbir şey değişmedi, hiçbir yan etki olmadı
            degismediDogrula(hedef, Role.USER);
            verifyNoInteractions(notificationRepository, refreshTokenService, passwordEncoder);
        }

        @Test
        @DisplayName("3) son admin + aktif sipariş → önce sipariş hatası; admin kilit sorgusu hiç çalışmaz")
        void aktifSiparis_sonAdminKontrolundenOnce() {
            // ARRANGE: hedef ADMIN ve aktif siparişi var
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(true);

            // ACT + ASSERT: sipariş kontrolü admin kontrolünden ÖNCE çalışıyor
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, null));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.USER_HAS_ACTIVE_ORDERS);

            // Kilit sorgusu hiç çalışmadı (gereksiz yere kilit tutulmadı)
            verify(userRepository, never()).findAllByRoleForUpdate(any());
            degismediDogrula(hedef, Role.ADMIN);
        }

        @Test
        @DisplayName("4) sıradan kullanıcı: anonimleşir, bildirimler silinir, tokenlar iptal edilir, admin sorgusu yok")
        void siradanKullanici_anonimlesir() {
            // ARRANGE
            User hedef = kullanici(HEDEF_ID, Role.USER);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            // anyString(): servis rastgele bir UUID metni verecek, değerini bilemeyiz; hangi metin gelirse gelsin
            // sahte hashleyici sabit bir hash döndürsün
            when(passwordEncoder.encode(anyString())).thenReturn(YENI_SIFRE_HASH);

            // ACT: çağrı öncesi ve sonrası an (deletedAt aralık kontrolü için)
            LocalDateTime once = LocalDateTime.now();
            userService.deleteUser(HEDEF_ID, null);
            LocalDateTime sonra = LocalDateTime.now();

            // ASSERT (1): kişisel veriler anonimleşti, rol düştü
            anonimlestiDogrula(hedef);
            assertThat(hedef.getDeletedAt()).isBetween(once, sonra);

            // ASSERT (2): hashlenen şey eski şifre değil, rastgele bir değer (eski şifre ile giriş artık mümkün değil)
            verify(passwordEncoder).encode(sifreCaptor.capture());
            assertThat(sifreCaptor.getValue()).isNotEqualTo(ESKI_SIFRE_HASH);

            // ASSERT (3): yan etkiler
            verify(notificationRepository).deleteByUserId(HEDEF_ID);
            verify(refreshTokenService).revokeAllForUser(HEDEF_ID);

            // ASSERT (4): admin değil → kilit sorgusu yok; devralan aranmadı (kullanıcı sorgusu yalnızca bir kez: hedef)
            verify(userRepository, never()).findAllByRoleForUpdate(any());
            verify(userRepository).findByIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("5) admin ama başka admin de var: successorId gerekmez, silinir")
        void adminAmaBaskaAdminVar_silinir() {
            // ARRANGE: kilitli sorgu 2 admin döndürüyor → hedef "son admin" değil
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            User digerAdmin = kullanici(DIGER_ADMIN_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef, digerAdmin));
            when(passwordEncoder.encode(anyString())).thenReturn(YENI_SIFRE_HASH);

            // ACT: successorId = null
            userService.deleteUser(HEDEF_ID, null);

            // ASSERT: hedef anonimleşti (rolü USER'a düştü), diğer admin dokunulmadan kaldı
            anonimlestiDogrula(hedef);
            assertThat(digerAdmin.getRole()).isEqualTo(Role.ADMIN);
            // Devralan aranmadı: kullanıcı sorgusu yalnızca hedef için yapıldı
            verify(userRepository).findByIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("6) son admin + successorId yok → 409 LAST_ADMIN_PROTECTED, hiçbir şey değişmez")
        void sonAdmin_successorYok_conflict() {
            // ARRANGE: kilitli sorgu yalnızca hedefi döndürüyor → son admin
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef));

            // ACT + ASSERT
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, null));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.LAST_ADMIN_PROTECTED);

            // Sistem yöneticisiz kalmadı: hedef olduğu gibi, yan etki yok
            degismediDogrula(hedef, Role.ADMIN);
            verifyNoInteractions(notificationRepository, refreshTokenService, passwordEncoder);
        }

        @Test
        @DisplayName("7) son admin + successorId kendisi → 400 VALIDATION_ERROR, hiçbir şey değişmez")
        void sonAdmin_successorKendisi_validationError() {
            // ARRANGE
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef));

            // ACT + ASSERT: kendi kendini devralamaz
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, HEDEF_ID));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.VALIDATION_ERROR);

            degismediDogrula(hedef, Role.ADMIN);
            verifyNoInteractions(notificationRepository, refreshTokenService, passwordEncoder);
            // Devralan için ayrı bir arama yapılmadı: kullanıcı sorgusu yalnızca bir kez (hedef)
            verify(userRepository).findByIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("8) son admin + devralan bulunamadı → 404, hedef değişmez")
        void sonAdmin_successorBulunamadi_notFound() {
            // ARRANGE: hedef bulunuyor, devralan aranınca bulunamıyor
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(userRepository.findByIdAndDeletedAtIsNull(VARIS_ID)).thenReturn(Optional.empty());
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef));

            // ACT + ASSERT
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(HEDEF_ID, VARIS_ID));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);

            // Devralan doğrulaması hedefin anonimleştirilmesinden ÖNCE yapılıyor: hedef hâlâ sağlam
            degismediDogrula(hedef, Role.ADMIN);
            verifyNoInteractions(notificationRepository, refreshTokenService, passwordEncoder);
        }

        @Test
        @DisplayName("9) son admin + geçerli devralan → devralan ADMIN olur, hedef anonimleşir")
        void sonAdmin_gecerliSuccessor_devredilir() {
            // ARRANGE: devralan sıradan bir USER
            User hedef = kullanici(HEDEF_ID, Role.ADMIN);
            User devralan = kullanici(VARIS_ID, Role.USER);
            when(userRepository.findByIdAndDeletedAtIsNull(HEDEF_ID)).thenReturn(Optional.of(hedef));
            when(userRepository.findByIdAndDeletedAtIsNull(VARIS_ID)).thenReturn(Optional.of(devralan));
            when(orderRepository.existsByUserIdAndStatusIn(HEDEF_ID, AKTIF_DURUMLAR)).thenReturn(false);
            when(userRepository.findAllByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(hedef));
            when(passwordEncoder.encode(anyString())).thenReturn(YENI_SIFRE_HASH);

            // ACT
            userService.deleteUser(HEDEF_ID, VARIS_ID);

            // ASSERT: yetki devredildi, eski admin anonimleşti, sistem yöneticisiz kalmadı
            assertThat(devralan.getRole()).isEqualTo(Role.ADMIN);
            anonimlestiDogrula(hedef);
            verify(notificationRepository).deleteByUserId(HEDEF_ID);
            verify(refreshTokenService).revokeAllForUser(HEDEF_ID);
        }
    }
}