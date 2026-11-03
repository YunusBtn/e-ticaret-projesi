package com.yunus.learning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndirimHesaplayiciTest {

    @Test
    void uygula_yuzde10_tutariYuzde10Dusurur() {
        //Arrange : test edilecek nesneler ve girdiler
        IndirimHesaplayici hesaplayici = new IndirimHesaplayici();
        BigDecimal tutar = new BigDecimal("200.00");

        BigDecimal sonuc = hesaplayici.uygula(tutar, 10);

        assertThat(sonuc).isEqualByComparingTo("180.00");
    }

    @Test
    void uygula_yuzde100denBuyuk_illegalArgumentFirlatir() {


        IndirimHesaplayici hesaplayici = new IndirimHesaplayici();

        assertThatThrownBy(() -> hesaplayici.uygula(new BigDecimal("50.00"), 150))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0-100");
    }


    class IndirimHesaplayici {

        BigDecimal uygula(BigDecimal tutar, int yuzde) {
            if (yuzde < 0 || yuzde > 100) {
                throw new IllegalArgumentException("Yüzde değeri 0-100 arasında olmalıdır");
            }
            BigDecimal indirim = tutar.multiply(BigDecimal.valueOf(yuzde))
                    .divide(BigDecimal.valueOf(100));
            return tutar.subtract(indirim);
        }


    }


}
