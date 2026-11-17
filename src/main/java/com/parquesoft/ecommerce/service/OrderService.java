package com.parquesoft.ecommerce.service;

import com.parquesoft.ecommerce.entity.OrderEntity;
import com.parquesoft.ecommerce.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "PAID", "CANCELLED");

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderEntity createOrder(OrderEntity order) {
        if (order == null) {
            throw new IllegalArgumentException("El pedido es obligatorio");
        }
        Double total = order.getTotalAmount();
        if (total == null || !Double.isFinite(total) || total <= 0) {
            throw new IllegalArgumentException("El monto total del pedido debe ser mayor a cero");
        }
        if (order.getCustomerId() == null) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
        }

        order.setId(null);
        order.setStatus("PENDING");
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(normalizeStatus(status));
    }

    @Transactional
    public OrderEntity updateOrderStatus(Long id, String newStatus) {
        String status = normalizeStatus(newStatus);
        OrderEntity order = findExistingOrder(id);
        if ("CANCELLED".equals(status)) {
            ensureCancellationAllowed(order);
        }
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long id) {
        OrderEntity order = findExistingOrder(id);
        ensureCancellationAllowed(order);
        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    private OrderEntity findExistingOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado con ID: " + id));
    }

    private void ensureCancellationAllowed(OrderEntity order) {
        if ("SHIPPED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("No se puede cancelar un pedido enviado");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || !VALID_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("El estado debe ser PENDING, PAID o CANCELLED");
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }
}
