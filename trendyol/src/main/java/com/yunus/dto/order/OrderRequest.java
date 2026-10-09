package com.yunus.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class OrderRequest {

    @NotEmpty(message = "Sipariş en az bir adet ürün içermelidir.")
    @Valid
    private List<OrderItemRequest> items;
}