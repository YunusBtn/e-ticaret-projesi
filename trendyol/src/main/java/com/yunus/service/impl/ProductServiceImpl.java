package com.yunus.service.impl;

import com.yunus.dto.common.PageResponse;
import com.yunus.dto.product.ProductRequest;
import com.yunus.dto.product.ProductResponse;
import com.yunus.entity.Product;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.ProductMapper;
import com.yunus.repository.ProductRepository;
import com.yunus.service.ProductService;
import com.yunus.util.PageableValidator;
import jakarta.persistence.Version;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Set<String> DEFAULT_SORT_FIELD = Set.of("id", "name", "price", "stockQuantity", "createdAt");


    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findActiveProductOrThrow(id));
    }


    @Transactional(readOnly = true)
    @Override
    public PageResponse<ProductResponse> getAllProducts(String name, Pageable pageable) {
        PageableValidator.validateSort(pageable, DEFAULT_SORT_FIELD);

        Page<Product> products = (name == null || name.isBlank())
                ? productRepository.findAllByDeletedAtIsNull(pageable)
                : productRepository.findAllByNameContainingIgnoreCaseAndDeletedAtIsNull(name, pageable);

        return PageResponse.from(products.map(productMapper::toResponse));
    }

    @Override
    @Transactional
    public void increaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> productNotFound(productId));
        product.setStockQuantity(product.getStockQuantity() + quantity);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existing = findActiveProductOrThrow(id);
        existing.setName(request.getName());
        existing.setPrice(request.getPrice());
        existing.setDescription(request.getDescription());
        existing.setStockQuantity(request.getStockQuantity());

        Product updated = productRepository.save(existing);
        return productMapper.toResponse(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = findActiveProductOrThrow(id);
        product.setDeletedAt(LocalDateTime.now());

    }

    @Override
    @Transactional
    public Product decreaseStockAndGetProduct(Long productId, int quantity) {
        Product product = findActiveProductOrThrow(productId);


        if (product.getStockQuantity() < quantity) {
            throw new BusinessException(ErrorType.INSUFFICIENT_STOCK,
                    "Ürün: " + product.getName()
                            + ", mevcut: " + product.getStockQuantity()
                            + ", istenen: " + quantity
                            + "Sonuç : Stok yetersiz");
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);

        return productRepository.save(product);
    }


    private Product findActiveProductOrThrow(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> productNotFound(id));
    }


    private BusinessException productNotFound(Long id) {
        return new BusinessException(ErrorType.NOT_FOUND, "ürün bulunamadı, id: " + id);
    }


}
