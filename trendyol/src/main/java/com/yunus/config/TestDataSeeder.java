package com.yunus.config;

import com.yunus.entity.Order;
import com.yunus.entity.OrderItem;
import com.yunus.entity.Product;
import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;
import com.yunus.enums.Role;
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
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class TestDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int USER_COUNT = 50;
    private static final int PRODUCT_COUNT = 40;
    private static final int ORDER_COUNT = 200;

    private static final String[] FIRST_NAMES = {
            "Ahmet", "Mehmet", "Ayse", "Fatma", "Ali", "Zeynep",
            "Can", "Elif", "Burak", "Deniz", "Emre", "Selin", "Mert", "Ece", "Kerem"
    };

    private static final String[] LAST_NAMES = {
            "Yilmaz", "Kaya", "Demir", "Celik", "Sahin",
            "Arslan", "Ozturk", "Aydin", "Polat", "Koc"
    };

    private static final String[] BRANDS = {
            "Asus", "Lenovo", "Samsung", "Apple", "Xiaomi", "Philips", "Sony"
    };

    private static final String[] PRODUCT_TYPES = {
            "Dizustu Bilgisayar", "Kulaklik", "Telefon", "Tablet",
            "Monitor", "Klavye", "Mouse", "Hoparlor"
    };

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 10) {
            log.info("Test verisi zaten mevcut, seed atlandı.");
            return;
        }

        List<User> users = seedUsers();
        List<Product> products = seedProducts();
        seedOrders(users, products);

        log.info(
                "Test verisi üretildi: {} kullanıcı, {} ürün, {} sipariş",
                USER_COUNT,
                PRODUCT_COUNT,
                ORDER_COUNT
        );

        log.info("Seed kullanıcılarının hepsinin şifresi: Test1234!");
    }

    private List<User> seedUsers() {
        String hashedPassword = passwordEncoder.encode("Test1234!");
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<User> users = new ArrayList<>();

        for (int i = 1; i <= USER_COUNT; i++) {
            String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];

            String username = (first + last).toLowerCase() + i;

            users.add(
                    User.builder()
                            .createdAt(LocalDateTime
                                    .now()
                                    .minusDays(random.nextInt(0, 365))
                                    .minusMinutes(random.nextInt(0, 1440)))
                            .username(username)
                            .email(username + "@example.com")
                            .password(hashedPassword)
                            .role(Role.USER)
                            .build()
            );
        }

        return userRepository.saveAll(users);
    }

    private List<Product> seedProducts() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Product> products = new ArrayList<>();

        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            String name = BRANDS[random.nextInt(BRANDS.length)] + " "
                    + PRODUCT_TYPES[random.nextInt(PRODUCT_TYPES.length)] + " " + i;

            products.add(
                    Product.builder()
                            .createdAt(LocalDateTime
                                    .now()
                                    .minusDays(random.nextInt(0, 365))
                                    .minusMinutes(random.nextInt(0, 1440)))
                            .name(name)
                            .description("Test ürünü: " + name)
                            .price(BigDecimal.valueOf(
                                    random.nextInt(1_000, 500_000),
                                    2
                            ))
                            .stockQuantity(random.nextInt(0, 200))
                            .build()
            );
        }

        return productRepository.saveAll(products);
    }

    private void seedOrders(List<User> users, List<Product> products) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        OrderStatus[] statuses = OrderStatus.values();
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < ORDER_COUNT; i++) {
            User user = users.get(random.nextInt(users.size()));

            LocalDateTime createdAt = LocalDateTime.now()
                    .minusDays(random.nextInt(0, 90))
                    .minusMinutes(random.nextInt(0, 1440));

            Order order = Order.builder()
                    .user(user)
                    .status(statuses[random.nextInt(statuses.length)])
                    .createdAt(createdAt)
                    .build();

            List<Product> pool = new ArrayList<>(products);
            Collections.shuffle(pool);

            int itemCount = random.nextInt(1, 5);

            BigDecimal total = BigDecimal.ZERO;

            for (Product product : pool.subList(0, itemCount)) {
                int quantity = random.nextInt(1, 4);

                order.getOrderItems().add(
                        OrderItem.builder()
                                .order(order)
                                .product(product)
                                .quantity(quantity)
                                .unitPrice(product.getPrice())
                                .build()
                );

                total = total.add(
                        product.getPrice()
                                .multiply(BigDecimal.valueOf(quantity))
                );
            }

            order.setTotalPrice(total);
            orders.add(order);
        }

        orderRepository.saveAll(orders);
    }
}