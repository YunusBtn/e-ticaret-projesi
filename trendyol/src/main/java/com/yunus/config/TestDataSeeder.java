// ============================================================
// config/TestDataSeeder.java
// AMAÇ: Pageable/sort/arama testleri için yüzlerce gerçekçi kayıt
// üretir. Uygulama açılışında BİR KEZ çalışır, veri varsa atlar.
// SADECE app.seed.enabled=true ise yüklenir.
// ============================================================
package com.yunus.config; // kendi paketine göre uyarla

import com.yunus.entity.Order;
import com.yunus.entity.OrderItem;
import com.yunus.entity.Product;
import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;   // senin projende enums paketi böyleydi (OrderServiceImpl importundan)
import com.yunus.enums.Role;          // Role de aynı pakette ise; değilse düzelt
import com.yunus.repository.OrderRepository;
import com.yunus.repository.ProductRepository;
import com.yunus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
@RequiredArgsConstructor
// havingValue = "true": property yoksa VEYA false ise bu bean hiç oluşturulmaz.
// Yani yanlışlıkla production'a gitse bile, bayrak açılmadıkça hiçbir şey yapmaz.
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class TestDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    // Üretilecek miktarlar: istediğin gibi değiştirebilirsin
    private static final int USER_COUNT = 50;
    private static final int PRODUCT_COUNT = 40;
    private static final int ORDER_COUNT = 200;

    // Gerçekçi görünmesi için küçük isim havuzları. ASCII kullandım, Türkçe
    // karakterler username/email'de sorun çıkarmasın diye
    private static final String[] FIRST_NAMES = {"Ahmet", "Mehmet", "Ayse", "Fatma", "Ali", "Zeynep",
            "Can", "Elif", "Burak", "Deniz", "Emre", "Selin", "Mert", "Ece", "Kerem"};
    private static final String[] LAST_NAMES = {"Yilmaz", "Kaya", "Demir", "Celik", "Sahin",
            "Arslan", "Ozturk", "Aydin", "Polat", "Koc"};
    private static final String[] BRANDS = {"Asus", "Lenovo", "Samsung", "Apple", "Xiaomi", "Philips", "Sony"};
    private static final String[] PRODUCT_TYPES = {"Dizustu Bilgisayar", "Kulaklik", "Telefon", "Tablet",
            "Monitor", "Klavye", "Mouse", "Hoparlor"};

    @Override
    @Transactional
    // @Transactional: tüm seed işlemi TEK transaction'da. Ortada bir hata olursa
    // yarım veri kalmaz, hepsi geri alınır. Aynı zamanda LAZY ilişkilerin
    // (order.getUser vb.) session açıkken güvenle kullanılmasını sağlar.
    public void run(String... args) {
        // İDEMPOTENT kontrol: admin seeder zaten 1 kullanıcı oluşturuyor, bu yüzden
        // eşiği 10 aldık. Bundan fazla kullanıcı varsa daha önce seed edilmiş demektir
        if (userRepository.count() > 10) {
            log.info("Test verisi zaten mevcut, seed atlandı.");
            return;
        }

        List<User> users = seedUsers();
        List<Product> products = seedProducts();
        seedOrders(users, products);

        log.info("Test verisi üretildi: {} kullanıcı, {} ürün, {} sipariş", USER_COUNT, PRODUCT_COUNT, ORDER_COUNT);
        log.info("Seed kullanıcılarının hepsinin şifresi: Test1234!");
    }

    private List<User> seedUsers() {
        // Şifreyi BİR KEZ hash'liyoruz (yukarıda anlattığım BCrypt yavaşlığı nedeniyle).
        // Her kullanıcı aynı hash'i paylaşır, ama login'de hepsi "Test1234!" ile girebilir
        String hashedPassword = passwordEncoder.encode("Test1234!");
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<User> users = new ArrayList<>();
        for (int i = 1; i <= USER_COUNT; i++) {
            String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            // sonuna i ekliyoruz: username ve email UNIQUE olduğu için çakışma olmasın
            String username = (first + last).toLowerCase() + i;

            users.add(User.builder()
                    .username(username)
                    .email(username + "@example.com")
                    .password(hashedPassword)
                    .role(Role.USER) // seed kullanıcılar normal USER, admin'i AdminDataInitializer üretiyor
                    .build());
        }
        // saveAll: tek tek save() yerine toplu kaydeder, daha hızlı.
        // Dönen liste ID'leri dolu halde, siparişlerde bu nesneleri kullanacağız
        return userRepository.saveAll(users);
    }

    private List<Product> seedProducts() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Product> products = new ArrayList<>();

        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            String name = BRANDS[random.nextInt(BRANDS.length)] + " "
                    + PRODUCT_TYPES[random.nextInt(PRODUCT_TYPES.length)] + " " + i;

            products.add(Product.builder()
                    .name(name)
                    .description("Test ürünü: " + name)
                    // valueOf(unscaledValue, scale): 150000 ve scale 2 → 1500.00
                    // 10.00 ile 5000.00 TL arası fiyat. BigDecimal kullanıyoruz (double değil)
                    // çünkü para birimlerinde yuvarlama hatası olmamalı
                    .price(BigDecimal.valueOf(random.nextInt(1_000, 500_000), 2))
                    .stockQuantity(random.nextInt(0, 200)) // bazıları 0 stok: "stokta yok" testi için faydalı
                    .build());
        }
        return productRepository.saveAll(products);
    }

    private void seedOrders(List<User> users, List<Product> products) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        OrderStatus[] statuses = OrderStatus.values();
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < ORDER_COUNT; i++) {
            User user = users.get(random.nextInt(users.size()));

            // Geçmiş 90 gün içinde rastgele bir tarih: sıralama (sort) testinde
            // createdAt'in gerçekten farklı değerler alması için. (Aşağıdaki
            // "Order entity düzeltmesi" bölümüne bak)
            LocalDateTime createdAt = LocalDateTime.now()
                    .minusDays(random.nextInt(0, 90))
                    .minusMinutes(random.nextInt(0, 1440));

            Order order = Order.builder()
                    .user(user)
                    .status(statuses[random.nextInt(statuses.length)])
                    .createdAt(createdAt)
                    .build();

            // Ürün havuzunu karıştırıp baştan 1-4 tanesini alıyoruz. Böylece
            // AYNI siparişte aynı ürün iki kez geçmiyor (gerçekçi değil olurdu)
            List<Product> pool = new ArrayList<>(products);
            Collections.shuffle(pool);
            int itemCount = random.nextInt(1, 5); // 1 ile 4 arası kalem

            BigDecimal total = BigDecimal.ZERO;
            for (Product product : pool.subList(0, itemCount)) {
                int quantity = random.nextInt(1, 4); // 1-3 adet

                order.getOrderItems().add(OrderItem.builder()
                        .order(order)
                        .product(product)
                        .quantity(quantity)
                        // OrderServiceImpl'deki aynı "snapshot" mantığı: fiyatı o anki
                        // üründen KOPYALIYORUZ
                        .unitPrice(product.getPrice())
                        .build());

                // OrderServiceImpl.createOrder'daki hesapla birebir aynı: fiyat * adet
                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            }

            order.setTotalPrice(total);
            orders.add(order);
        }
        // cascade = ALL sayesinde her Order kaydedilirken içindeki OrderItem'lar da yazılır
        orderRepository.saveAll(orders);
    }
}