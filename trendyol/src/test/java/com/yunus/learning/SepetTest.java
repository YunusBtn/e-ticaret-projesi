package com.yunus.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.List;

/** Notlar :
//Test metodları private ya da static olamaz.
//@BeforeAll ve @AfterAll metotları static olmalıdır.
//Void dönmelidir.
*/

@DisplayName("Sepet")
class SepetTest {


    private Sepet sepet;






    






    @DisplayName("ekle()")
    @Nested // gruplandırma için ayrı bir test sınıfı için alt kategori
    class Ekle {

        @Test
        @DisplayName("geçerli bir ürün eklenince adet artar")
        void gecerliUrun_adetArtar(){
            sepet.ekle("kulaklık");
            assertThat(sepet.adet()).isEqualTo(1);
            assertThat(sepet.urunler()).containsExactly("kulaklık");

        }
    }

    @DisplayName("Aynı ürün iki kez eklenirse ikisi de sepette durur")
    @Test
    void ayniUrunIkiKez_ikisiDeEklenir(){
        sepet.ekle("Kalem");
        sepet.ekle("Kalem");
        assertThat(sepet.urunler()).hasSize(2).containsOnly("Kalem");
    }


    class Sepet{
        private final List<String> urunler = new ArrayList<>();


        void ekle(String urun){
            if (urun == null || urun.isBlank()){
                throw new IllegalArgumentException("Ürün adı boş olamaz!");
            }
            urunler.add(urun);
        }

        int adet(){return urunler.size();}
        boolean bosMu(){return urunler.isEmpty();}
        List<String> urunler(){return List.copyOf(urunler);}
    }



}


