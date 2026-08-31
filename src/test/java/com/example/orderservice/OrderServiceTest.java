package com.example.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;

@SpringBootTest
class OrderServiceTest {

	@Autowired
	private OrderService service;

	@Autowired
	private OrderRepository repository;

	@BeforeEach
	void clearStore() {
		repository.deleteAll();
	}

	private Order sampleOrder() {
		return new Order("Alice", "Laptop", 2, new BigDecimal("500.00"));
	}

	@Test
	void createsOrderWithCreatedStatusAndTotal() {
		Order created = service.create(sampleOrder());

		assertThat(created.getId()).isNotNull();
		assertThat(created.getStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(created.getTotalAmount()).isEqualByComparingTo("1000.00");
		assertThat(created.getCreatedAt()).isNotNull();
	}

	@Test
	void findsOrdersByStatus() {
		Order confirmed = service.create(sampleOrder());
		service.create(sampleOrder());
		service.updateStatus(confirmed.getId(), OrderStatus.CONFIRMED);

		assertThat(service.findAll(OrderStatus.CREATED)).hasSize(1);
		assertThat(service.findAll(OrderStatus.CONFIRMED)).containsExactly(confirmed);
		assertThat(service.findAll(null)).hasSize(2);
	}

	@Test
	void walksTheHappyPathThroughEveryStatus() {
		Order order = service.create(sampleOrder());

		service.updateStatus(order.getId(), OrderStatus.CONFIRMED);
		service.updateStatus(order.getId(), OrderStatus.SHIPPED);
		Order delivered = service.updateStatus(order.getId(), OrderStatus.DELIVERED);

		assertThat(delivered.getStatus()).isEqualTo(OrderStatus.DELIVERED);
	}

	@Test
	void rejectsIllegalStatusTransition() {
		Order order = service.create(sampleOrder());

		assertThatThrownBy(() -> service.updateStatus(order.getId(), OrderStatus.DELIVERED))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("Cannot move order");
	}

	@Test
	void rejectsUpdateOfCancelledOrder() {
		Order order = service.create(sampleOrder());
		service.updateStatus(order.getId(), OrderStatus.CANCELLED);

		assertThatThrownBy(() -> service.update(order.getId(), sampleOrder()))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("can no longer be modified");
	}

	@Test
	void deleteRemovesOrderAndThenFails() {
		Order order = service.create(sampleOrder());

		service.delete(order.getId());

		assertThat(repository.count()).isZero();
		assertThatThrownBy(() -> service.findById(order.getId()))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("not found");
	}
}
