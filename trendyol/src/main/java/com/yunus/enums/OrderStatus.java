package com.yunus.enums;

public enum OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED, CANCELLED;


    public boolean canTransitionTo(OrderStatus target) {

        return switch (this) {
            case PENDING -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }


}
