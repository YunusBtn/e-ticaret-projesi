package com.yunus.learning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

@DisplayName("Bilet Durumu Geçiş Kuralları")
class BiletDurumuTest {


    @ParameterizedTest(name = "{0} -> {1} : {2}")
    @CsvSource(textBlock = """
            # mevcut,     hedef,       izinli mi?
            REZERVE,      REZERVE,     false
            REZERVE,      ODENDI,      true
            REZERVE,      KULLANILDI,  false
            REZERVE,      IPTAL,       true
            # ödenmiş bilet yalnızca kullanılabilir
            ODENDI,       REZERVE,     false
            ODENDI,       ODENDI,      false
            ODENDI,       KULLANILDI,  true
            ODENDI,       IPTAL,       false
            # son durumlar: hiçbir yere gidemez
            KULLANILDI,   REZERVE,     false
            KULLANILDI,   ODENDI,      false
            KULLANILDI,   KULLANILDI,  false
            KULLANILDI,   IPTAL,       false
            IPTAL,        REZERVE,     false
            IPTAL,        ODENDI,      false
            IPTAL,        KULLANILDI,  false
            IPTAL,        IPTAL,       false
            """)
    @DisplayName("TÜM GEÇİŞ MATRİSİ")
    void gecebilirMi_tumKombinasyonları(
            BiletDurumu mevcut,
            BiletDurumu hedef,
            boolean beklenen) {

        assertThat(mevcut.gecebilirMi(hedef)).isEqualTo(beklenen);
    }


    @ParameterizedTest(name = "{0} son durumdur, hiçbir duruma geçemez")
    @EnumSource(value = BiletDurumu.class, names = {"KULLANILDI", "IPTAL"})
    void sonDurum_hicbirDurumaGecemez(BiletDurumu sonDurum) {
        assertThat(Arrays.stream(BiletDurumu.values()).noneMatch(sonDurum::gecebilirMi)); //hedef -> sonDurum.gecebilirMi(hedef)


    }


    enum BiletDurumu {
        REZERVE, ODENDI, KULLANILDI, IPTAL;

        boolean gecebilirMi(BiletDurumu hedef) {
            return switch (this) {
                case REZERVE -> hedef == ODENDI || hedef == IPTAL;
                case ODENDI -> hedef == KULLANILDI;
                case IPTAL, KULLANILDI -> false;

            };
        }
    }


}
