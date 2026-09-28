package com.yunus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.yunus.entity")
@EnableJpaRepositories("com.yunus.repository")
public class TrendyolApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrendyolApplication.class, args);
    }

}
