package com.yunus.learning;


import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class KuponServisiTest {


    class Kupon {
        private final String kod;
        private boolean kullanildi;

        public Kupon(String kod, boolean kullanildi) {
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
                    .orElseThrow(()-> new IllegalArgumentException("Kupon bulunamadı : " + kod));
            if (kupon.isKullanildi()) {
                throw new IllegalStateException("Kupon zaten kullanıldı: " + kod);
            }
            kupon.kullanildiOlarakIsaretle();
            kuponRepository.save(kupon);
            bildirimGonderici.gonder(kullaniciId, "Kuponunuz kullanıldı : " +  kod);
        }


    }












}
