package com.yunus.repository;

import com.yunus.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Product> findByStockQuantityGreaterThan(Integer quantity);


    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Product> findAllByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

}
