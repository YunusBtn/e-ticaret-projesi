package com.yunus.learning;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

/**
 * Notlar :
 * //Test metodları private ya da static olamaz.
 * //@BeforeAll ve @AfterAll metotları static olmalıdır.
 * //Void dönmelidir.
 */

@DisplayName("Sepet")
class SepetTest {


    private Sepet sepet;

    @BeforeAll
    static void tumTestlerdenOnce() {
        System.out.println("[BeforeAll]SepetTestlerdenOnce(1kez)");

    }

    @BeforeEach
    void hazirla() {
        sepet = new Sepet();
        System.out.println("[BeforeEach]SepetTestlerdenOnce(1kez)");
    }


    @AfterEach
    void temizle(){
        System.out.println("[AfterEach]  Testler Bitti");
    }



    @AfterAll
    static void tumTestlerdenSonra(){
        System.out.println("[AfterAll]  Sınıf bitti");
    }

    @Test
    @DisplayName("yeni sepet boştur")
    void yeniSepet_bostur(){
        assertThat(sepet.bosMu()).isTrue();
        assertThat(sepet.adet()).isZero();

    }

    @DisplayName("ekle()")
    @Nested // gruplandırma için ayrı bir test sınıfı için alt kategori
    class Ekle {

        @Test
        @DisplayName("geçerli bir ürün eklenince adet artar")
        void gecerliUrun_adetArtar() {
            sepet.ekle("kulaklık");
            assertThat(sepet.adet()).isEqualTo(1);
            assertThat(sepet.urunler()).containsExactly("kulaklık");

        }
    }

    @DisplayName("Aynı ürün iki kez eklenirse ikisi de sepette durur")
    @Test
    void ayniUrunIkiKez_ikisiDeEklenir() {
        sepet.ekle("Kalem");
        sepet.ekle("Kalem");
        assertThat(sepet.urunler()).hasSize(2).containsOnly("Kalem");
    }


    @ParameterizedTest(name = "[{index}] ad=\"{0}\" -> hata fırlatır " )
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void gecersizAd_hataFirlatir(String ad) {
        assertThatThrownBy(() -> sepet.ekle(ad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş");
        assertThat(sepet.bosMu()).isTrue();
    }


    class Sepet {
        private final List<String> urunler = new ArrayList<>();


        void ekle(String urun) {
            if (urun == null || urun.isBlank()) {
                throw new IllegalArgumentException("Ürün adı boş olamaz!");
            }
            urunler.add(urun);
        }

        int adet() {
            return urunler.size();
        }

        boolean bosMu() {
            return urunler.isEmpty();
        }

        List<String> urunler() {
            return List.copyOf(urunler);
        }
    }


}


