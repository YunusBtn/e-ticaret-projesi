package com.yunus.repository;

import com.yunus.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStockQuantityGreaterThan(Integer quantity);

    List<Product> findByNameContainingIgnoreCase(String name);

}
