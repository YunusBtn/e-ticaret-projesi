package com.yunus.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderStatus Geçiş kuralları")
class OrderStatusTest {


    @ParameterizedTest(name = "{0} -> {1} : {2}")
    @CsvSource(textBlock = """
            # mevcut,    hedef,      izinli mi?
            # PENDING: ödenebilir veya iptal edilebilir
            PENDING,     PENDING,    false
            PENDING,     PAID,       true
            PENDING,     SHIPPED,    false
            PENDING,     DELIVERED,  false
            PENDING,     CANCELLED,  true
            # PAID: yalnızca kargoya verilebilir (iptal YOK)
            PAID,        PENDING,    false
            PAID,        PAID,       false
            PAID,        SHIPPED,    true
            PAID,        DELIVERED,  false
            PAID,        CANCELLED,  false
            # SHIPPED: yalnızca teslim edilebilir
            SHIPPED,     PENDING,    false
            SHIPPED,     PAID,       false
            SHIPPED,     SHIPPED,    false
            SHIPPED,     DELIVERED,  true
            SHIPPED,     CANCELLED,  false
            # DELIVERED: son durum
            DELIVERED,   PENDING,    false
            DELIVERED,   PAID,       false
            DELIVERED,   SHIPPED,    false
            DELIVERED,   DELIVERED,  false
            DELIVERED,   CANCELLED,  false
            # CANCELLED: son durum
            CANCELLED,   PENDING,    false
            CANCELLED,   PAID,       false
            CANCELLED,   SHIPPED,    false
            CANCELLED,   DELIVERED,  false
            CANCELLED,   CANCELLED,  false
            """)
    @DisplayName("Tüm Geçiş Matrisi")
    void canTransitionTo_tumKombinasyonlar(OrderStatus mevcut, OrderStatus hedef, boolean beklenen) {

        assertThat(mevcut.canTransitionTo(hedef)).isEqualTo(beklenen);

    }

    @ParameterizedTest(name = "{0} son durumdur, hiçbir duruma geçemez")
    @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED"})
    void sonDurum_hicbirDurumaGecemez(OrderStatus sonDurum) {
        assertThat(OrderStatus.values()).noneMatch(sonDurum::canTransitionTo);
    }


    @Test
    @DisplayName("NULL hedef hata fırlatmadan false döner")
    void canTransitionTo_nullHedef_falseDoner() {
        assertThat(OrderStatus.PENDING.canTransitionTo(null)).isFalse();
    }


}
