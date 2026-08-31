package com.example.orderservice.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService service;

	public OrderController(OrderService service) {
		this.service = service;
	}

	/** GET /api/orders?status=CREATED */
	@GetMapping
	public List<Order> list(@RequestParam(required = false) OrderStatus status) {
		return service.findAll(status);
	}

	@GetMapping("/{id}")
	public Order get(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	public ResponseEntity<Order> create(@Valid @RequestBody Order order) {
		Order created = service.create(order);
		return ResponseEntity.created(URI.create("/api/orders/" + created.getId())).body(created);
	}

	@PutMapping("/{id}")
	public Order update(@PathVariable Long id, @Valid @RequestBody Order order) {
		return service.update(id, order);
	}

	/** PATCH /api/orders/{id}/status?value=CONFIRMED */
	@PatchMapping("/{id}/status")
	public Order updateStatus(@PathVariable Long id, @RequestParam("value") OrderStatus value) {
		return service.updateStatus(id, value);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	/** Handy for confirming which pod served the request after a rollout. */
	@GetMapping("/stats")
	public Map<String, Object> stats() {
		return Map.of(
				"totalOrders", service.count(),
				"statuses", OrderStatus.values());
	}
}
