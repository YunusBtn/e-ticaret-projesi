package com.yunus.service;import com.yunus.entity.RefreshToken;
import com.yunus.entity.User;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.repository.RefreshTokenRepository;import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;   import org.springframework.test.util.ReflectionTestUtils;   import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;   import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {private static final String HAM_TOKEN = "abc";
    private static final String HAM_TOKEN_HASH =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    private static final Long TOKEN_ID = 100L;
    private static final String AILE_ID = "11111111-1111-1111-1111-111111111111";
    private static final long YENILEME_SURESI_MS = 604_800_000L;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Captor
    ArgumentCaptor<RefreshToken> tokenCaptor;

    @Captor
    ArgumentCaptor<LocalDateTime> zamanCaptor;

    @InjectMocks
    RefreshTokenService refreshTokenService;

    @BeforeEach
    void ayarla() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", YENILEME_SURESI_MS);
    }

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
        return User.builder().id(1L).build();
    }

    private static String sha256(String ham) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(ham.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    @DisplayName("rotate()")
    class Rotate {

        @Test
        @DisplayName("token veritabanında yok → AUTHENTICATION_REQUIRED, hiçbir şey yazılmaz")
        void tokenYok_hata() {
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.empty());

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            verify(refreshTokenRepository, never()).markUsed(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("token iptal edilmiş → AUTHENTICATION_REQUIRED, markUsed çağrılmaz")
        void tokenIptalEdilmis_hata() {
            LocalDateTime simdi = LocalDateTime.now();
            RefreshToken iptal = token(aktifKullanici(), simdi.plusDays(1), null, simdi.minusMinutes(5));
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(iptal));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            verify(refreshTokenRepository, never()).markUsed(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("token süresi dolmuş → AUTHENTICATION_REQUIRED")
        void tokenSuresiDolmus_hata() {
            LocalDateTime simdi = LocalDateTime.now();
            RefreshToken eski = token(aktifKullanici(), simdi.minusMinutes(1), null, null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(eski));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            verify(refreshTokenRepository, never()).markUsed(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("token sahibi kullanıcı silinmiş → AUTHENTICATION_REQUIRED")
        void kullaniciSilinmis_hata() {
            LocalDateTime simdi = LocalDateTime.now();
            User silinmis = User.builder().id(1L).deletedAt(simdi.minusDays(1)).build();
            RefreshToken gecerli = token(silinmis, simdi.plusDays(1), null, null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(gecerli));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            verify(refreshTokenRepository, never()).markUsed(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("bilinmeyen ve iptal edilmiş token AYNI mesajı verir (istemci nedeni ayıramaz)")
        void bilinmeyenVeIptalEdilmis_ayniMesaj() {
            LocalDateTime simdi = LocalDateTime.now();
            RefreshToken iptal = token(aktifKullanici(), simdi.plusDays(1), null, simdi.minusMinutes(5));
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH))
                    .thenReturn(Optional.of(iptal), Optional.empty());

            BusinessException iptalHatasi = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            BusinessException yokHatasi = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));

            assertThat(iptalHatasi.getErrorType()).isEqualTo(yokHatasi.getErrorType());
            assertThat(iptalHatasi.getMessage()).isEqualTo(yokHatasi.getMessage());
        }

        @Test
        @DisplayName("REPLAY: markUsed 0 dönerse (token zaten kullanılmış) tüm aile iptal edilir, yeni token üretilmez")
        void tokenYenidenKullanildi_aileIptalEdilir() {
            LocalDateTime simdi = LocalDateTime.now();
            RefreshToken kullanilmis = token(aktifKullanici(), simdi.plusDays(1), simdi.minusMinutes(1), null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(kullanilmis));
            when(refreshTokenRepository.markUsed(eq(TOKEN_ID), any(LocalDateTime.class))).thenReturn(0);

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> refreshTokenService.rotate(HAM_TOKEN));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.AUTHENTICATION_REQUIRED);

            InOrder sira = inOrder(refreshTokenRepository);
            sira.verify(refreshTokenRepository).markUsed(eq(TOKEN_ID), any(LocalDateTime.class));
            sira.verify(refreshTokenRepository).revokeFamily(eq(AILE_ID), any(LocalDateTime.class));

            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("başarı: eski token kullanıldı işaretlenir, AYNI aileyle yeni token hash'li saklanır, ham token döner")
        void basarili_yeniTokenAyniAileyle() {
            User kullanici = aktifKullanici();
            LocalDateTime hazirlik = LocalDateTime.now();
            RefreshToken eski = token(kullanici, hazirlik.plusDays(1), null, null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(eski));
            when(refreshTokenRepository.markUsed(eq(TOKEN_ID), any(LocalDateTime.class))).thenReturn(1);

            LocalDateTime once = LocalDateTime.now();
            RefreshTokenService.RotatedToken sonuc = refreshTokenService.rotate(HAM_TOKEN);
            LocalDateTime sonra = LocalDateTime.now();

            InOrder sira = inOrder(refreshTokenRepository);
            sira.verify(refreshTokenRepository).markUsed(eq(TOKEN_ID), zamanCaptor.capture());
            sira.verify(refreshTokenRepository).save(tokenCaptor.capture());
            assertThat(zamanCaptor.getValue()).isBetween(once, sonra);

            assertThat(sonuc.user()).isSameAs(kullanici);
            assertThat(sonuc.refreshToken()).matches("[A-Za-z0-9_-]{43}");

            RefreshToken yeni = tokenCaptor.getValue();
            assertThat(yeni.getUser()).isSameAs(kullanici);
            assertThat(yeni.getFamilyId()).isEqualTo(AILE_ID);
            assertThat(yeni.getTokenHash()).isNotEqualTo(sonuc.refreshToken());
            assertThat(yeni.getTokenHash()).isEqualTo(sha256(sonuc.refreshToken()));
            assertThat(yeni.getTokenHash()).isNotEqualTo(HAM_TOKEN_HASH);
            Duration omur = Duration.ofMillis(YENILEME_SURESI_MS);
            assertThat(yeni.getExpiresAt()).isBetween(once.plus(omur), sonra.plus(omur));
        }
    }

    @Nested
    @DisplayName("createForLogin()")
    class CreateForLogin {

        @Test
        @DisplayName("her giriş YENİ bir aile açar; ham token döner, veritabanına hash'i yazılır")
        void herGirisYeniAile() {
            User kullanici = aktifKullanici();

            LocalDateTime once = LocalDateTime.now();
            String ham1 = refreshTokenService.createForLogin(kullanici);
            String ham2 = refreshTokenService.createForLogin(kullanici);
            LocalDateTime sonra = LocalDateTime.now();

            verify(refreshTokenRepository, times(2)).save(tokenCaptor.capture());
            List<RefreshToken> kayitlar = tokenCaptor.getAllValues();
            RefreshToken ilk = kayitlar.get(0);
            RefreshToken ikinci = kayitlar.get(1);

            assertThat(ham1).isNotEqualTo(ham2);
            assertThat(ilk.getFamilyId()).isNotEqualTo(ikinci.getFamilyId());
            UUID.fromString(ilk.getFamilyId());
            UUID.fromString(ikinci.getFamilyId());
            assertThat(ilk.getUser()).isSameAs(kullanici);
            assertThat(ilk.getTokenHash()).isEqualTo(sha256(ham1));
            assertThat(ikinci.getTokenHash()).isEqualTo(sha256(ham2));
            assertThat(ilk.getExpiresAt())
                    .isBetween(once.plus(Duration.ofMillis(YENILEME_SURESI_MS)),
                            sonra.plus(Duration.ofMillis(YENILEME_SURESI_MS)));
        }
    }

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("token varsa ailesinin tamamı iptal edilir")
        void tokenVar_aileIptalEdilir() {
            RefreshToken mevcut = token(aktifKullanici(), LocalDateTime.now().plusDays(1), null, null);
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.of(mevcut));

            refreshTokenService.logout(HAM_TOKEN);

            verify(refreshTokenRepository).revokeFamily(eq(AILE_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("token yoksa sessizce geçer, hata fırlatmaz ve iptal yapılmaz")
        void tokenYok_sessizGecer() {
            when(refreshTokenRepository.findByTokenHash(HAM_TOKEN_HASH)).thenReturn(Optional.empty());

            refreshTokenService.logout(HAM_TOKEN);

            verify(refreshTokenRepository, never()).revokeFamily(any(), any());
        }
    }

    @Nested
    @DisplayName("revokeAllForUser()")
    class RevokeAllForUser {

        @Test
        @DisplayName("kullanıcının tüm tokenları şimdiki zamanla iptal edilir")
        void tumTokenlarIptalEdilir() {
            LocalDateTime once = LocalDateTime.now();
            refreshTokenService.revokeAllForUser(5L);
            LocalDateTime sonra = LocalDateTime.now();

            verify(refreshTokenRepository).revokeAllByUserId(eq(5L), zamanCaptor.capture());
            assertThat(zamanCaptor.getValue()).isBetween(once, sonra);
        }
    }

}