package com.yunus.service;
// Test edilen LoginAttemptService ile AYNI paket (com.yunus.service), ama src/TEST/java altında.

// ---------- JUnit ----------
import org.junit.jupiter.api.BeforeEach;                    // her testten önce çalışır
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;         // tablo satırlarıyla parametreli test

import static org.assertj.core.api.Assertions.assertThat;

/*
 * LoginAttemptService için BİRİM testleri.
 *
 * Bu testte MOCK YOK ve Mockito/Spring da yok: servisin dış bağımlılığı yok, gerçek nesneyi doğrudan
 * oluşturuyoruz. Bu yüzden testler çok hızlı ve Mockito agent uyarısı bu dosyada çıkmaz.
 *
 * Kanıtlanmayanlar:
 *  - Kilidin 15 dakika sonra kalkması: süre ve cache servisin içinde sabit, dışarıdan verilemiyor.
 *  - merge'ün eşzamanlı isteklerde doğru sayması: Bölüm C3'te (paralel test tekniği) ele alınacak.
 */
@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

    private static final String ALI = "ali";
    private static final String VELI = "veli";

    private LoginAttemptService servis;

    // JUnit her test için sınıfın yeni bir örneğini oluşturduğundan alan zaten her testte taze olurdu.
    // @BeforeEach ile bunu açıkça yazmak, "her testin kendi temiz servisi var" bilgisini görünür yapar.
    @BeforeEach
    void hazirla() {
        servis = new LoginAttemptService();   // sayaçlar boş
    }

    // Aynı kullanıcı için art arda n başarısız deneme kaydeder (testi kısa tutmak için).
    private void basarisizDene(String kullanici, int kez) {
        for (int i = 0; i < kez; i++) {
            servis.recordFailure(kullanici);
        }
    }

    @ParameterizedTest(name = "{0} başarısız deneme → bloklu mu: {1}")
    @CsvSource({
            "0,  false",   // hiç deneme yok (yeni kullanıcı)
            "1,  false",
            "4,  false",   // eşiğin HEMEN ALTI
            "5,  true",    // TAM EŞİK: 5. başarısız denemede kilit devreye girmeli
            "6,  true",    // eşiğin üstünde kilit devam eder
            "20, true"
    })
    void esikDegeri_kilitDurumu(int denemeSayisi, boolean beklenenBlok) {
        // ARRANGE + ACT: n kez başarısız deneme
        basarisizDene(ALI, denemeSayisi);

        // ASSERT: eşik 5; 4'te açık, 5'te kilitli
        assertThat(servis.isBlocked(ALI)).isEqualTo(beklenenBlok);
    }

    @Test
    @DisplayName("başarılı giriş sayacı sıfırlar: 4 hata + başarı + 4 hata kilitlemez, sıfırdan 5. hata kilitler")
    void basariSayaciSifirlar() {
        // ACT (1): 4 hata, sonra başarılı giriş
        basarisizDene(ALI, 4);
        servis.recordSuccess(ALI);

        // ACT (2): 4 hata daha. Sayaç sıfırlanmasaydı toplam 8 olurdu ve kilit devreye girerdi.
        basarisizDene(ALI, 4);
        assertThat(servis.isBlocked(ALI)).isFalse();

        // ASSERT: sayım gerçekten SIFIRDAN başlamış: sıfırlamadan sonraki 5. hata kilitler
        servis.recordFailure(ALI);
        assertThat(servis.isBlocked(ALI)).isTrue();
    }

    @Test
    @DisplayName("bloklu hesapta recordSuccess kilidi kaldırır (davranış belgeleme: kilit kontrolü çağıranın işi)")
    void blokluHesaptaBasari_kilidiKaldirir() {
        // ARRANGE: hesap kilitli
        basarisizDene(ALI, 5);
        assertThat(servis.isBlocked(ALI)).isTrue();

        // ACT: bir şekilde başarı kaydedildi
        servis.recordSuccess(ALI);

        // ASSERT: servis kendi içinde başarıyı REDDETMİYOR, kilit kalkıyor.
        // Bu yüzden login akışı kilitli kullanıcıyı şifre doğrulamasından ÖNCE reddetmek zorunda.
        assertThat(servis.isBlocked(ALI)).isFalse();
    }

    @Test
    @DisplayName("kullanıcılar birbirinden bağımsız: biri kilitlenince diğeri etkilenmez")
    void kullanicilarBagimsiz() {
        // ACT: yalnızca ali kilitleniyor, veli'nin 1 hatası var
        basarisizDene(ALI, 5);
        basarisizDene(VELI, 1);

        // ASSERT
        assertThat(servis.isBlocked(ALI)).isTrue();
        assertThat(servis.isBlocked(VELI)).isFalse();
    }
}