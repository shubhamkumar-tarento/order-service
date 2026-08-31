package com.example.orderservice.model;

/**
 * Lifecycle of an order. Transitions are validated in OrderService.
 */
public enum OrderStatus {
	CREATED,
	CONFIRMED,
	SHIPPED,
	DELIVERED,
	CANCELLED
}
