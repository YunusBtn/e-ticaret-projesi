package com.yunus.service;
// Test edilen RefreshTokenService ile AYNI paket (com.yunus.service), ama src/TEST/java altında.

// ---------- Projenin kendi sınıfları ----------
import com.yunus.entity.RefreshToken;                  // GERÇEK entity
import com.yunus.entity.User;                          // GERÇEK entity
import com.yunus.exception.BusinessException;          // servisin fırlattığı hata
import com.yunus.exception.ErrorType;                  // AUTHENTICATION_REQUIRED
import com.yunus.repository.RefreshTokenRepository;    // SAHTE: token tablosu

// ---------- JUnit ----------
import org.junit.jupiter.api.BeforeEach;               // her testten önce çalışır
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// ---------- Mockito ----------
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// ---------- Spring ----------
import org.springframework.test.util.ReflectionTestUtils;   // private alana değer atar (@Value yerine)

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * RefreshTokenService için BİRİM testleri.
 *
 * Mock'un ispatlayamadıkları (Bölüm C'ye bırakıldı):
 *  - rotate'taki noRollbackFor sayesinde "aile iptali" hata fırlatılsa bile GERÇEKTEN kalıcı oluyor mu?
 *  - markUsed'ın atomik UPDATE'i iki eşzamanlı istekten yalnızca birine 1 döndürüyor mu?
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    // ==================================================================================
    // SABİTLER
    // ==================================================================================
    private static final String HAM_TOKEN = "abc";
    // SHA-256("abc"): standartta sabit, bağımsız bir referans değer. Stub'ı bununla kuruyoruz:
    // servis farklı bir algoritma/kodlama kullanırsa stub eşleşmez ve test kırılır.
    private static final String HAM_TOKEN_HASH =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    private static final Long TOKEN_ID = 100L;
    private static final String AILE_ID = "11111111-1111-1111-1111-111111111111";
    private static final long YENILEME_SURESI_MS = 604_800_000L;   // 7 gün

    // ==================================================================================
    // SAHTE BAĞIMLILIK ve GERÇEK SERVİS
    // ==================================================================================
    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Captor
    ArgumentCaptor<RefreshToken> tokenCaptor;   // save'e giden yeni token

    @Captor
    ArgumentCaptor<LocalDateTime> zamanCaptor;  // repository'ye giden "şimdi" değeri

    @InjectMocks
    RefreshTokenService refreshTokenService;

    // @Value("${jwt.refresh-expiration}") alanını Spring doldurur; birim testte Spring yok, alan 0 kalır.
    // @InjectMocks yapıcı enjeksiyonu kullandığında alanlara ayrıca dokunmaz. Bu yüzden elle dolduruyoruz.
    // MockitoExtension @InjectMocks nesnesini bu metottan ÖNCE hazırlar, yani nesne burada kullanıma hazır.
    // Alan adı değişirse (refreshExpirationMs) bu satır IllegalArgumentException ile testi kırar.
    @BeforeEach
    void ayarla() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", YENILEME_SURESI_MS);
    }

    // ==================================================================================
    // TEST VERİSİ YARDIMCILARI
    // ==================================================================================

    // Veritabanından gelmiş gibi bir token. Kullanıcı, süre, kullanılma ve iptal zamanını dışarıdan veriyoruz.
    private static RefreshToken token(User kullanici, LocalDateTime sonaErer,
                                      LocalDateTime kullanildi, LocalDateTime iptalEdildi) {
        return RefreshToken.builder()
                .id(TOKEN_ID)
                .user(kullanici)
                .tokenHash(HAM_TOKEN_HASH)
                .familyId(AILE_ID)
                .expiresAt(sonaErer)
                .usedAt(kullanildi)
                .revokedAt(iptalEdildi)
                .build();
    }

    private static User aktifKullanici() {
        return User.builder().id(1L).build();   // deletedAt null → aktif
    }

    // Servisin ürettiği ham token'ın hash'ini bağımsız olarak hesaplar: "saklanan hash, döndürülen ham
    // token'ın hash'i mi?" sorusunu cevaplamak için. (Yanlış hash saklansaydı yeni token hiç kullanılamazdı.)
    private static String sha256(String ham) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(ham.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ==================================================================================
    // rotate()
    // ==================================================================================
    @Nested
    @DisplayName("rotate()")
    class Rotate {

        @Test
        @DisplayName("token veritabanında yok → AUTHENTICATION_REQUIRED, hiçbir şey yazılmaz")
        void tokenYok_hata() {
            // ARRANGE: stub yalnızca "abc"in SHA-256 hash'i ile eşleşir (hash algoritmasını da kanıtlar)
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.empty());

            // ACT + ASSERT
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            verify(refreshTokenRepository, never()).markUsed(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("token iptal edilmiş → AUTHENTICATION_REQUIRED, markUsed çağrılmaz")
        void tokenIptalEdilmis_hata() {
            // ARRANGE: iptal zamanı dolu
            LocalDateTime simdi = LocalDateTime.now();
            RefreshToken iptal = token(aktifKullanici(), simdi.plusDays(1), null, simdi.minusMinutes(5));
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(iptal));

            // ACT + ASSERT
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            verify(refreshTokenRepository, never()).markUsed(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("token süresi dolmuş → AUTHENTICATION_REQUIRED")
        void tokenSuresiDolmus_hata() {
            // ARRANGE: bitiş zamanı geçmişte
            LocalDateTime simdi = LocalDateTime.now();
            RefreshToken eski = token(aktifKullanici(), simdi.minusMinutes(1), null, null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(eski));

            // ACT + ASSERT
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            verify(refreshTokenRepository, never()).markUsed(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("token sahibi kullanıcı silinmiş → AUTHENTICATION_REQUIRED")
        void kullaniciSilinmis_hata() {
            // ARRANGE: token geçerli görünüyor ama kullanıcının silinme zamanı dolu
            LocalDateTime simdi = LocalDateTime.now();
            User silinmis = User.builder().id(1L).deletedAt(simdi.minusDays(1)).build();
            RefreshToken gecerli = token(silinmis, simdi.plusDays(1), null, null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(gecerli));

            // ACT + ASSERT
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            verify(refreshTokenRepository, never()).markUsed(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("bilinmeyen ve iptal edilmiş token AYNI mesajı verir (istemci nedeni ayıramaz)")
        void bilinmeyenVeIptalEdilmis_ayniMesaj() {
            // ARRANGE: ilk çağrıda iptal edilmiş token, ikinci çağrıda boş (art arda stub)
            LocalDateTime simdi = LocalDateTime.now();
            RefreshToken iptal = token(aktifKullanici(), simdi.plusDays(1), null, simdi.minusMinutes(5));
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH))
                    .thenReturn(Optional.of(iptal), Optional.empty());

            // ACT
            BusinessException iptalHatasi = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            BusinessException yokHatasi = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));

            // ASSERT: tür ve mesaj birebir aynı
            assertThat(iptalHatasi.getErrorType()).isEqualTo(yokHatasi.getErrorType());
            assertThat(iptalHatasi.getMessage()).isEqualTo(yokHatasi.getMessage());
        }

        @Test
        @DisplayName("REPLAY: markUsed 0 dönerse (token zaten kullanılmış) tüm aile iptal edilir, yeni token üretilmez")
        void tokenYenidenKullanildi_aileIptalEdilir() {
            // ARRANGE: token daha önce kullanılmış (usedAt dolu) ama iptal edilmemiş ve süresi dolmamış.
            // Servis usedAt'e BAKMAZ; karar markUsed'ın döndürdüğü satır sayısına bağlı.
            // Gerçek veritabanında "where used_at is null" şartı yüzünden güncellenen satır 0 olur.
            LocalDateTime simdi = LocalDateTime.now();
            RefreshToken kullanilmis = token(aktifKullanici(), simdi.plusDays(1), simdi.minusMinutes(1), null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(kullanilmis));
            when(refreshTokenRepository.markUsed(eq(TOKEN_ID), any(LocalDateTime.class))).thenReturn(0);

            // ACT + ASSERT (1): aynı genel hata döner
            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            // ASSERT (2): önce markUsed denendi, sonra TÜM AİLE iptal edildi
            InOrder sira = inOrder(refreshTokenRepository);
            sira.verify(refreshTokenRepository).markUsed(eq(TOKEN_ID), any(LocalDateTime.class));
            sira.verify(refreshTokenRepository).revokeFamily(eq(AILE_ID), any(LocalDateTime.class));

            // ASSERT (3): yeni token üretilmedi
            verify(refreshTokenRepository, never()).save(any());

            // Not: revokeFamily'nin hata fırlatıldıktan sonra da KALICI olması noRollbackFor'a bağlı.
            // Bu birim testle kanıtlanamaz, Bölüm C'de gerçek veritabanıyla kanıtlayacağız.
        }

        @Test
        @DisplayName("başarı: eski token kullanıldı işaretlenir, AYNI aileyle yeni token hash'li saklanır, ham token döner")
        void basarili_yeniTokenAyniAileyle() {
            // ARRANGE
            User kullanici = aktifKullanici();
            LocalDateTime hazirlik = LocalDateTime.now();
            RefreshToken eski = token(kullanici, hazirlik.plusDays(1), null, null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(eski));
            // markUsed 1 satır güncelledi → bu istek token'ı "ilk kullanan" oldu
            when(refreshTokenRepository.markUsed(eq(TOKEN_ID), any(LocalDateTime.class))).thenReturn(1);

            // ACT
            LocalDateTime once = LocalDateTime.now();
            RefreshTokenService.RotatedToken sonuc = refreshTokenService.rotate(HAM_TOKEN);
            LocalDateTime sonra = LocalDateTime.now();

            // ASSERT (1): sıra: önce eski token kullanıldı işaretlenir, SONRA yenisi kaydedilir
            InOrder sira = inOrder(refreshTokenRepository);
            sira.verify(refreshTokenRepository).markUsed(eq(TOKEN_ID), zamanCaptor.capture());
            sira.verify(refreshTokenRepository).save(tokenCaptor.capture());
            assertThat(zamanCaptor.getValue()).isBetween(once, sonra);

            // ASSERT (2): dönen değer: kullanıcı aynı nesne, ham token 32 bayt URL-güvenli Base64 (43 karakter, dolgu yok)
            assertThat(sonuc.user()).isSameAs(kullanici);
            assertThat(sonuc.refreshToken()).matches("[A-Za-z0-9_-]{43}");

            // ASSERT (3): kaydedilen yeni token
            RefreshToken yeni = tokenCaptor.getValue();
            assertThat(yeni.getUser()).isSameAs(kullanici);
            // Aile devam ediyor: replay tespiti aileyi bu zincir üzerinden iptal edebilir
            assertThat(yeni.getFamilyId()).isEqualTo(AILE_ID);
            // Veritabanında HAM değil, HASH var; ve bu hash döndürülen ham token'a ait
            assertThat(yeni.getTokenHash()).isNotEqualTo(sonuc.refreshToken());
            assertThat(yeni.getTokenHash()).isEqualTo(sha256(sonuc.refreshToken()));
            assertThat(yeni.getTokenHash()).isNotEqualTo(HAM_TOKEN_HASH);   // eski token'ı tekrar üretmedi
            // Yeni token'ın ömrü: şimdi + 7 gün
            Duration omur = Duration.ofMillis(YENILEME_SURESI_MS);
            assertThat(yeni.getExpiresAt()).isBetween(once.plus(omur), sonra.plus(omur));
        }
    }

    // ==================================================================================
    // createForLogin()
    // ==================================================================================
    @Nested
    @DisplayName("createForLogin()")
    class CreateForLogin {

        @Test
        @DisplayName("her giriş YENİ bir aile açar; ham token döner, veritabanına hash'i yazılır")
        void herGirisYeniAile() {
            // ARRANGE
            User kullanici = aktifKullanici();

            // ACT: aynı kullanıcı iki ayrı cihazdan giriş yapıyor
            LocalDateTime once = LocalDateTime.now();
            String ham1 = refreshTokenService.createForLogin(kullanici);
            String ham2 = refreshTokenService.createForLogin(kullanici);
            LocalDateTime sonra = LocalDateTime.now();

            // ASSERT: save iki kez çağrıldı; tüm çağrıların argümanlarını sırayla alıyoruz
            verify(refreshTokenRepository, times(2)).save(tokenCaptor.capture());
            List<RefreshToken> kayitlar = tokenCaptor.getAllValues();
            RefreshToken ilk = kayitlar.get(0);
            RefreshToken ikinci = kayitlar.get(1);

            assertThat(ham1).isNotEqualTo(ham2);                       // iki giriş farklı token
            assertThat(ilk.getFamilyId()).isNotEqualTo(ikinci.getFamilyId());   // farklı aile
            UUID.fromString(ilk.getFamilyId());                         // aile kimliği geçerli bir UUID
            UUID.fromString(ikinci.getFamilyId());
            assertThat(ilk.getUser()).isSameAs(kullanici);
            assertThat(ilk.getTokenHash()).isEqualTo(sha256(ham1));     // hash'li saklama
            assertThat(ikinci.getTokenHash()).isEqualTo(sha256(ham2));
            assertThat(ilk.getExpiresAt())
                    .isBetween(once.plus(Duration.ofMillis(YENILEME_SURESI_MS)),
                            sonra.plus(Duration.ofMillis(YENILEME_SURESI_MS)));
        }
    }

    // ==================================================================================
    // logout()
    // ==================================================================================
    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("token varsa ailesinin tamamı iptal edilir")
        void tokenVar_aileIptalEdilir() {
            // ARRANGE
            RefreshToken mevcut = token(aktifKullanici(), LocalDateTime.now().plusDays(1), null, null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(mevcut));

            // ACT
            refreshTokenService.logout(HAM_TOKEN);

            // ASSERT: yalnızca bu token değil, tüm aile (diğer cihazların zinciri aynı aile değildir ama bu zincir tamamen kapanır)
            verify(refreshTokenRepository).revokeFamily(eq(AILE_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("token yoksa sessizce geçer, hata fırlatmaz ve iptal yapılmaz")
        void tokenYok_sessizGecer() {
            // ARRANGE
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.empty());

            // ACT: çıkış işlemi tekrarlanabilir olmalı (zaten çıkış yapmış kullanıcı tekrar tıklayabilir)
            refreshTokenService.logout(HAM_TOKEN);

            // ASSERT
            verify(refreshTokenRepository, never()).revokeFamily(any(), any());
        }
    }

    // ==================================================================================
    // revokeAllForUser()
    // ==================================================================================
    @Nested
    @DisplayName("revokeAllForUser()")
    class RevokeAllForUser {

        @Test
        @DisplayName("kullanıcının tüm tokenları şimdiki zamanla iptal edilir")
        void tumTokenlarIptalEdilir() {
            // ACT
            LocalDateTime once = LocalDateTime.now();
            refreshTokenService.revokeAllForUser(5L);
            LocalDateTime sonra = LocalDateTime.now();

            // ASSERT: doğru kullanıcı, doğru zaman aralığı
            verify(refreshTokenRepository).revokeAllByUserId(eq(5L), zamanCaptor.capture());
            assertThat(zamanCaptor.getValue()).isBetween(once, sonra);
        }
    }
}