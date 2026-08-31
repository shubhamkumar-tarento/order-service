package com.example.orderservice.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class Order {

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Long id;

	@NotBlank(message = "customerName is required")
	private String customerName;

	@NotBlank(message = "product is required")
	private String product;

	@Min(value = 1, message = "quantity must be at least 1")
	private int quantity;

	@NotNull(message = "price is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
	private BigDecimal price;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private OrderStatus status;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Instant createdAt;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Instant updatedAt;

	public Order() {
	}

	public Order(String customerName, String product, int quantity, BigDecimal price) {
		this.customerName = customerName;
		this.product = product;
		this.quantity = quantity;
		this.price = price;
	}

	/** Derived field, exposed in JSON responses but never accepted as input. */
	public BigDecimal getTotalAmount() {
		if (price == null) {
			return BigDecimal.ZERO;
		}
		return price.multiply(BigDecimal.valueOf(quantity));
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public String toString() {
		return "Order{id=" + id + ", customerName='" + customerName + "', product='" + product
				+ "', quantity=" + quantity + ", price=" + price + ", status=" + status + "}";
	}
}
