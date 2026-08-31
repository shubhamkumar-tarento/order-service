package com.example.orderservice.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;

/**
 * In-memory store. Everything lives in a ConcurrentHashMap, so state is lost
 * on restart - fine for a demo/testing deployment, and it keeps the pod
 * stateless enough to scale for smoke tests.
 */
@Repository
public class OrderRepository {

	private final Map<Long, Order> store = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong(0);

	public List<Order> findAll() {
		List<Order> orders = new ArrayList<>(store.values());
		orders.sort(Comparator.comparing(Order::getId));
		return orders;
	}

	public List<Order> findByStatus(OrderStatus status) {
		return findAll().stream()
				.filter(order -> order.getStatus() == status)
				.toList();
	}

	public Optional<Order> findById(Long id) {
		return Optional.ofNullable(store.get(id));
	}

	/** Assigns an id when the order is new, then stores it. */
	public Order save(Order order) {
		if (order.getId() == null) {
			order.setId(sequence.incrementAndGet());
		}
		store.put(order.getId(), order);
		return order;
	}

	public boolean deleteById(Long id) {
		return store.remove(id) != null;
	}

	public boolean existsById(Long id) {
		return store.containsKey(id);
	}

	public long count() {
		return store.size();
	}

	public void deleteAll() {
		store.clear();
	}
}
