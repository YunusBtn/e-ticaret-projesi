package com.yunus.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KuponServisi")
class KuponServisiTest {

    @Mock
    KuponRepository kuponRepository;

    @Mock
    BildirimGonderici bildirimGonderici;

    @InjectMocks
    KuponServisi kuponServisi;

    @Captor
    ArgumentCaptor<Kupon> kuponCaptor;

    @Test
    @DisplayName("geçerli kupon kullanılınca 'kullanıldı' olarak kaydedilir ve bildirim gider")
    void kuponKullan_gecerliKupon_kaydedilirVeBildirimGider() {
        Kupon kupon = new Kupon("INDIRIM10", false);
        when(kuponRepository.findByKod("INDIRIM10")).thenReturn(Optional.of(kupon));

        kuponServisi.kuponKullan("INDIRIM10", 42L);

        verify(kuponRepository).save(kuponCaptor.capture());
        assertThat(kuponCaptor.getValue().isKullanildi()).isTrue();
        verify(bildirimGonderici).gonder(eq(42L), contains("INDIRIM10"));
    }

    @Test
    @DisplayName("kupon bulunamazsa hata fırlar, hiçbir şey kaydedilmez ve bildirim gitmez")
    void kuponKullan_kuponYok_hataFirlarVeYanEtkiYok() {
        when(kuponRepository.findByKod("YOK")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kuponServisi.kuponKullan("YOK", 42L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bulunamadı");

        verify(kuponRepository, never()).save(any());
        verifyNoInteractions(bildirimGonderici);
    }

    @Test
    @DisplayName("kupon zaten kullanılmışsa hata fırlar ve tekrar kaydedilmez")
    void kuponKullan_kullanilmisKupon_hataFirlarVeKayitYok() {
        when(kuponRepository.findByKod("ESKI")).thenReturn(Optional.of(new Kupon("ESKI", true)));

        assertThatThrownBy(() -> kuponServisi.kuponKullan("ESKI", 42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zaten");

        verify(kuponRepository, never()).save(any());
        verifyNoInteractions(bildirimGonderici);
    }
}

class Kupon {
    private final String kod;
    private boolean kullanildi;

    Kupon(String kod, boolean kullanildi) {
        this.kod = kod;
        this.kullanildi = kullanildi;
    }

    String getKod() {
        return kod;
    }

    boolean isKullanildi() {
        return kullanildi;
    }

    void kullanildiOlarakIsaretle() {
        this.kullanildi = true;
    }
}

interface KuponRepository {
    Optional<Kupon> findByKod(String kod);

    Kupon save(Kupon kupon);
}

interface BildirimGonderici {
    void gonder(Long kullaniciId, String mesaj);
}

class KuponServisi {
    private final KuponRepository kuponRepository;
    private final BildirimGonderici bildirimGonderici;

    KuponServisi(KuponRepository kuponRepository, BildirimGonderici bildirimGonderici) {
        this.kuponRepository = kuponRepository;
        this.bildirimGonderici = bildirimGonderici;
    }

    void kuponKullan(String kod, Long kullaniciId) {
        Kupon kupon = kuponRepository.findByKod(kod)
                .orElseThrow(() -> new IllegalArgumentException("Kupon bulunamadı: " + kod));
        if (kupon.isKullanildi()) {
            throw new IllegalStateException("Kupon zaten kullanılmış: " + kod);
        }
        kupon.kullanildiOlarakIsaretle();
        kuponRepository.save(kupon);
        bildirimGonderici.gonder(kullaniciId, "Kuponunuz kullanıldı: " + kod);
    }
}