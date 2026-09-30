package com.yunus.service.impl;

import com.yunus.dto.product.ProductRequest;
import com.yunus.dto.product.ProductResponse;
import com.yunus.entity.Product;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.ProductMapper;
import com.yunus.repository.ProductRepository;
import com.yunus.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Ürün Bulunamadı"));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Ürün bulunamadı"));
        existing.setName(request.getName());
        existing.setPrice(request.getPrice());
        existing.setDescription(request.getDescription());
        existing.setStockQuantity(request.getStockQuantity());

        Product updated = productRepository.save(existing);
        return productMapper.toResponse(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Silinecek ürün bulunmadı"));
        productRepository.delete(product);

    }

    @Override
    public Product decreaseStockAndGetProduct(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "ürün bulunamadı"));

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
}
