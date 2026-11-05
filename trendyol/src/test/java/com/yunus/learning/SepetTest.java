package com.yunus.learning;

import org.junit.jupiter.api.DisplayName;

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


