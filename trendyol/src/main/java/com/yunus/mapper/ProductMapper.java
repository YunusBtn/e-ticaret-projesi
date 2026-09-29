package com.yunus.mapper;

import com.yunus.dto.product.ProductRequest;
import com.yunus.dto.product.ProductResponse;
import com.yunus.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {


    Product toEntity(ProductRequest request);
    ProductResponse toResponse(Product product);

}
