package com.yunus.service.impl;

import com.yunus.dto.payment.PaymentRequest;
import com.yunus.entity.Order;
import com.yunus.entity.Payment;
import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;
import com.yunus.enums.PaymentMethod;
import com.yunus.enums.PaymentStatus;
import com.yunus.enums.Role;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.PaymentMapper;
import com.yunus.model.UserPrincipal;
import com.yunus.repository.PaymentRepository;

import com.yunus.service.OrderService;
import com.yunus.service.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl")
class PaymentServiceImplTest {

    private static final Long SAHIP_ID = 1L;
    private static final Long BASKA_KULLANICI_ID = 2L;
    private static final Long SIPARIS_ID = 10L;
    private static final BigDecimal SIPARIS_TUTARI = new BigDecimal("100.00");
    private static final PaymentMethod ODEME_YONTEMI = PaymentMethod.values()[0];

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    PaymentMapper paymentMapper;

    @Mock
    OrderService orderService;

    @Captor
    ArgumentCaptor<Payment> paymentCaptor;

    @InjectMocks
    PaymentServiceImpl paymentService;

    private final UserPrincipal sahip = principal(SAHIP_ID);
    private final UserPrincipal yabanci = principal(BASKA_KULLANICI_ID);

    private static UserPrincipal principal(Long id) {
        return new UserPrincipal(User.builder().id(id).role(Role.USER).build());
    }

    private static Order siparis(OrderStatus durum) {
        return Order.builder()
                .id(SIPARIS_ID)
                .status(durum)
                .totalPrice(SIPARIS_TUTARI)
                .build();
    }

    private static PaymentRequest odemeIstegi(String tutar) {
        PaymentRequest istek = new PaymentRequest();
        istek.setOrderId(SIPARIS_ID);
        istek.setAmount(new BigDecimal(tutar));
        istek.setMethod(ODEME_YONTEMI);
        return istek;
    }

    @Nested
    @DisplayName("createPayment()")
    class CreatePayment {

        @Test
        @DisplayName("geçerli ödeme: doğru alanlarla kaydedilir, sipariş PAID yapılır, kaydedilmiş ödeme dönüştürülür")
        void gecerliOdeme_kaydedilir_siparisOdenir() {
            Order order = siparis(OrderStatus.PENDING);
            when(orderService.getOrderEntityForOwner(SIPARIS_ID, sahip)).thenReturn(order);
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.empty());
            Payment kaydedilmisOdeme = Payment.builder().id(50L).build();
            when(paymentRepository.save(any(Payment.class))).thenReturn(kaydedilmisOdeme);

            LocalDateTime once = LocalDateTime.now();
            paymentService.createPayment(odemeIstegi("100.00"), sahip);
            LocalDateTime sonra = LocalDateTime.now();

            verify(paymentRepository).save(paymentCaptor.capture());
            Payment kaydedilen = paymentCaptor.getValue();
            assertThat(kaydedilen.getOrder()).isSameAs(order);
            assertThat(kaydedilen.getAmount()).isEqualByComparingTo("100.00");
            assertThat(kaydedilen.getMethod()).isEqualTo(ODEME_YONTEMI);
            assertThat(kaydedilen.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(kaydedilen.getPaidAt()).isBetween(once, sonra);

            verify(orderService).markAsPaid(SIPARIS_ID);
            verify(paymentMapper).toResponse(kaydedilmisOdeme);
        }

        @Test
        @DisplayName("sıra: sahiplik → çift ödeme kontrolü → kayıt → markAsPaid")
        void kontrolSirasi_dogru() {
            when(orderService.getOrderEntityForOwner(SIPARIS_ID, sahip)).thenReturn(siparis(OrderStatus.PENDING));
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.empty());

            paymentService.createPayment(odemeIstegi("100.00"), sahip);

            InOrder sira = inOrder(orderService, paymentRepository);
            sira.verify(orderService).getOrderEntityForOwner(SIPARIS_ID, sahip);
            sira.verify(paymentRepository).findByOrderId(SIPARIS_ID);
            sira.verify(paymentRepository).save(any(Payment.class));
            sira.verify(orderService).markAsPaid(SIPARIS_ID);
        }

        @Test
        @DisplayName("tutarın ölçeği farklı olsa da (100.0 vs 100.00) değer aynıysa ödeme kabul edilir")
        void olcekFarkli_degerAyni_kabulEdilir() {
            when(orderService.getOrderEntityForOwner(SIPARIS_ID, sahip)).thenReturn(siparis(OrderStatus.PENDING));
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.empty());

            paymentService.createPayment(odemeIstegi("100.0"), sahip);

            verify(orderService).markAsPaid(SIPARIS_ID);
        }

        @Test
        @DisplayName("başkasının ÖDENMİŞ siparişi bile 404 verir (409 değil), ödeme tablosuna hiç bakılmaz")
        void baskasininOdenmisSiparisi_notFound_409Degil() {
            BusinessException bulunamadi = new BusinessException(ErrorType.NOT_FOUND, "Sipariş bulunamadı, id: " + SIPARIS_ID);
            when(orderService.getOrderEntityForOwner(SIPARIS_ID, yabanci)).thenThrow(bulunamadi);

            lenient().when(paymentRepository.findByOrderId(SIPARIS_ID))
                    .thenReturn(Optional.of(Payment.builder().id(5L).build()));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> paymentService.createPayment(odemeIstegi("100.00"), yabanci));
            assertThat(hata).isSameAs(bulunamadi);

            verify(paymentRepository, never()).findByOrderId(any());
            verify(paymentRepository, never()).save(any());
            verify(orderService, never()).markAsPaid(any());
        }

        @Test
        @DisplayName("siparişin ödemesi zaten varsa 409 DUPLICATE_ENTRY, yeni ödeme kaydedilmez")
        void odemeZatenVar_duplicateEntry() {
            when(orderService.getOrderEntityForOwner(SIPARIS_ID, sahip)).thenReturn(siparis(OrderStatus.PENDING));
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.of(Payment.builder().id(5L).build()));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> paymentService.createPayment(odemeIstegi("100.00"), sahip));

            assertThat(hata.getErrorType()).isEqualTo(ErrorType.DUPLICATE_ENTRY);
            verify(paymentRepository, never()).save(any());
            verify(orderService, never()).markAsPaid(any());
        }

        @ParameterizedTest(name = "{0} durumundaki sipariş ödenemez → 400 INVALID_PAYMENT")
        @EnumSource(value = OrderStatus.class, names = {"PENDING"}, mode = EnumSource.Mode.EXCLUDE)
        void siparisPendingDegil_invalidPayment(OrderStatus durum) {
            when(orderService.getOrderEntityForOwner(SIPARIS_ID, sahip)).thenReturn(siparis(durum));
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.empty());

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> paymentService.createPayment(odemeIstegi("100.00"), sahip));

            assertThat(hata.getErrorType()).isEqualTo(ErrorType.INVALID_PAYMENT);
            verify(paymentRepository, never()).save(any());
        }

        @ParameterizedTest(name = "tutar {0} (beklenen 100.00) → 400 INVALID_PAYMENT")
        @ValueSource(strings = {"99.99", "100.01", "1000.00"})
        void tutarUyusmuyor_invalidPayment(String yanlisTutar) {
            when(orderService.getOrderEntityForOwner(SIPARIS_ID, sahip)).thenReturn(siparis(OrderStatus.PENDING));
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.empty());

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> paymentService.createPayment(odemeIstegi(yanlisTutar), sahip));

            assertThat(hata.getErrorType()).isEqualTo(ErrorType.INVALID_PAYMENT);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("kayıt sırasında unique ihlali (eşzamanlı ikinci ödeme) → 409, sipariş PAID yapılmaz")
        void kayittaUniqueIhlali_duplicateEntry_siparisOdenmez() {
            when(orderService.getOrderEntityForOwner(SIPARIS_ID, sahip)).thenReturn(siparis(OrderStatus.PENDING));
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class)))
                    .thenThrow(new DataIntegrityViolationException("payments.order_id unique ihlali"));

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> paymentService.createPayment(odemeIstegi("100.00"), sahip));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.DUPLICATE_ENTRY);

            verify(orderService, never()).markAsPaid(any());
            verifyNoInteractions(paymentMapper);
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId()")
    class GetPaymentByOrderId {

        @Test
        @DisplayName("erişim yoksa hata aynen iletilir, ödeme tablosuna bakılmaz")
        void erisimYok_hataIletilir() {
            BusinessException bulunamadi = new BusinessException(ErrorType.NOT_FOUND, "Sipariş bulunamadı, id: " + SIPARIS_ID);
            when(orderService.getOrderEntityForAccess(SIPARIS_ID, yabanci)).thenThrow(bulunamadi);

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> paymentService.getPaymentByOrderId(SIPARIS_ID, yabanci));
            assertThat(hata).isSameAs(bulunamadi);
            verifyNoInteractions(paymentRepository);
        }

        @Test
        @DisplayName("siparişin ödeme kaydı yoksa 404 NOT_FOUND")
        void odemeKaydiYok_notFound() {
            when(orderService.getOrderEntityForAccess(SIPARIS_ID, sahip)).thenReturn(siparis(OrderStatus.PENDING));
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.empty());

            BusinessException hata = assertThrows(BusinessException.class,
                    () -> paymentService.getPaymentByOrderId(SIPARIS_ID, sahip));
            assertThat(hata.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verifyNoInteractions(paymentMapper);
        }

        @Test
        @DisplayName("erişim varsa ve ödeme varsa ödeme dönüştürülür")
        void erisimVeOdemeVar_donusturulur() {
            Payment odeme = Payment.builder().id(50L).build();
            when(orderService.getOrderEntityForAccess(SIPARIS_ID, sahip)).thenReturn(siparis(OrderStatus.PAID));
            when(paymentRepository.findByOrderId(SIPARIS_ID)).thenReturn(Optional.of(odeme));

            paymentService.getPaymentByOrderId(SIPARIS_ID, sahip);

            verify(paymentMapper).toResponse(odeme);
        }
    }
}