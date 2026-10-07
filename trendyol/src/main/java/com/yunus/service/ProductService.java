package com.yunus.service;

import com.yunus.dto.common.PageResponse;
import com.yunus.dto.product.ProductRequest;
import com.yunus.dto.product.ProductResponse;
import com.yunus.entity.Product;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    Product decreaseStockAndGetProduct(Long productId, int quantity);

    PageResponse<ProductResponse> getAllProducts(Pageable pageable);
}
