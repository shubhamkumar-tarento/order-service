package com.example.orderservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.OrderRepository;

import jakarta.annotation.PostConstruct;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	/** Which statuses an order is allowed to move to from its current one. */
	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
			OrderStatus.CREATED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
			OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
			OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED),
			OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
			OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

	private final OrderRepository repository;

	@Value("${order.seed-sample-data:true}")
	private boolean seedSampleData;

	public OrderService(OrderRepository repository) {
		this.repository = repository;
	}

	/** Gives a freshly deployed pod something to return on GET /api/orders. */
	@PostConstruct
	void seed() {
		if (!seedSampleData) {
			return;
		}
		create(new Order("Alice", "Laptop", 1, new BigDecimal("1200.00")));
		create(new Order("Bob", "Mechanical Keyboard", 2, new BigDecimal("89.50")));
		log.info("Seeded {} sample orders", repository.count());
	}

	public List<Order> findAll(OrderStatus status) {
		return status == null ? repository.findAll() : repository.findByStatus(status);
	}

	public Order findById(Long id) {
		return repository.findById(id).orElseThrow(() -> notFound(id));
	}

	public Order create(Order order) {
		Instant now = Instant.now();
		order.setId(null);
		order.setStatus(OrderStatus.CREATED);
		order.setCreatedAt(now);
		order.setUpdatedAt(now);
		Order saved = repository.save(order);
		log.info("Created order {}", saved.getId());
		return saved;
	}

	/** Replaces the editable fields; id, status and createdAt are preserved. */
	public Order update(Long id, Order changes) {
		Order existing = findById(id);
		if (existing.getStatus() == OrderStatus.DELIVERED || existing.getStatus() == OrderStatus.CANCELLED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Order " + id + " is " + existing.getStatus() + " and can no longer be modified");
		}
		existing.setCustomerName(changes.getCustomerName());
		existing.setProduct(changes.getProduct());
		existing.setQuantity(changes.getQuantity());
		existing.setPrice(changes.getPrice());
		existing.setUpdatedAt(Instant.now());
		log.info("Updated order {}", id);
		return repository.save(existing);
	}

	public Order updateStatus(Long id, OrderStatus newStatus) {
		Order existing = findById(id);
		OrderStatus current = existing.getStatus();
		if (current == newStatus) {
			return existing;
		}
		if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Cannot move order " + id + " from " + current + " to " + newStatus);
		}
		existing.setStatus(newStatus);
		existing.setUpdatedAt(Instant.now());
		log.info("Order {} moved from {} to {}", id, current, newStatus);
		return repository.save(existing);
	}

	public void delete(Long id) {
		if (!repository.deleteById(id)) {
			throw notFound(id);
		}
		log.info("Deleted order {}", id);
	}

	public long count() {
		return repository.count();
	}

	private ResponseStatusException notFound(Long id) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found");
	}
}
