package com.yunus.service;

import org.junit.jupiter.api.BeforeEach;                    // her testten önce çalışır
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;         // tablo satırlarıyla parametreli test

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

    private static final String ALI = "ali";
    private static final String VELI = "veli";

    private LoginAttemptService servis;

    @BeforeEach
    void hazirla() {
        servis = new LoginAttemptService();
    }

    private void basarisizDene(String kullanici, int kez) {
        for (int i = 0; i < kez; i++) {
            servis.recordFailure(kullanici);
        }
    }

    @ParameterizedTest(name = "{0} başarısız deneme → bloklu mu: {1}")
    @CsvSource({
            "0,  false",
            "1,  false",
            "4,  false",
            "5,  true",
            "6,  true",
            "20, true"
    })
    void esikDegeri_kilitDurumu(int denemeSayisi, boolean beklenenBlok) {
        basarisizDene(ALI, denemeSayisi);

        assertThat(servis.isBlocked(ALI)).isEqualTo(beklenenBlok);
    }

    @Test
    @DisplayName("başarılı giriş sayacı sıfırlar: 4 hata + başarı + 4 hata kilitlemez, sıfırdan 5. hata kilitler")
    void basariSayaciSifirlar() {
        basarisizDene(ALI, 4);
        servis.recordSuccess(ALI);

        basarisizDene(ALI, 4);
        assertThat(servis.isBlocked(ALI)).isFalse();

        servis.recordFailure(ALI);
        assertThat(servis.isBlocked(ALI)).isTrue();
    }

    @Test
    @DisplayName("bloklu hesapta recordSuccess kilidi kaldırır (davranış belgeleme: kilit kontrolü çağıranın işi)")
    void blokluHesaptaBasari_kilidiKaldirir() {
        basarisizDene(ALI, 5);
        assertThat(servis.isBlocked(ALI)).isTrue();

        servis.recordSuccess(ALI);

        assertThat(servis.isBlocked(ALI)).isFalse();
    }

    @Test
    @DisplayName("kullanıcılar birbirinden bağımsız: biri kilitlenince diğeri etkilenmez")
    void kullanicilarBagimsiz() {
        basarisizDene(ALI, 5);
        basarisizDene(VELI, 1);

        assertThat(servis.isBlocked(ALI)).isTrue();
        assertThat(servis.isBlocked(VELI)).isFalse();
    } }