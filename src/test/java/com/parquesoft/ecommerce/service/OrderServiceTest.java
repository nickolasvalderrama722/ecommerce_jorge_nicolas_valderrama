package com.parquesoft.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parquesoft.ecommerce.entity.OrderEntity;
import com.parquesoft.ecommerce.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testCreateOrder_Success() {
        OrderEntity order = new OrderEntity();
        order.setId(99L);
        order.setCustomerId(1L);
        order.setTotalAmount(185000.50);
        order.setStatus("PAID");
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(call -> call.getArgument(0));

        OrderEntity saved = orderService.createOrder(order);

        assertSame(order, saved);
        assertEquals("PENDING", saved.getStatus());
        assertNotNull(saved.getOrderDate());
        assertNull(saved.getId());
        verify(orderRepository).save(order);
    }

    @Test
    void testCreateOrder_ThrowsException_WhenTotalIsZeroOrNegative() {
        for (double invalidAmount : new double[] {0.0, -50.0}) {
            OrderEntity order = new OrderEntity();
            order.setCustomerId(1L);
            order.setTotalAmount(invalidAmount);

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> orderService.createOrder(order));
            assertEquals("El monto total del pedido debe ser mayor a cero", error.getMessage());
        }
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void testCreateOrder_RejectsMissingCustomer() {
        OrderEntity order = new OrderEntity();
        order.setTotalAmount(10.0);
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(order));
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void testCreateOrder_PreservesProvidedDate() {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(1L);
        order.setTotalAmount(10.0);
        LocalDateTime date = LocalDateTime.of(2026, 11, 13, 19, 30);
        order.setOrderDate(date);
        when(orderRepository.save(order)).thenReturn(order);

        assertEquals(date, orderService.createOrder(order).getOrderDate());
    }

    @Test
    void testGetOrderById_Success() {
        OrderEntity order = new OrderEntity();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertSame(order, orderService.getOrderById(1L).orElseThrow());
    }

    @Test
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(orderService.getOrderById(99L).isEmpty());
    }

    @Test
    void cancelOrderRejectsShippedWithoutSaving() {
        OrderEntity shipped = new OrderEntity();
        shipped.setStatus("SHIPPED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(shipped));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L));
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void cancelOrderUpdatesExistingOrder() {
        OrderEntity order = new OrderEntity();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(1L);

        assertEquals("CANCELLED", order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatusRejectsMissingOrder() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> orderService.updateOrderStatus(99L, "PAID"));
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void updateOrderStatusCannotBypassShippedCancellationRule() {
        OrderEntity shipped = new OrderEntity();
        shipped.setStatus("SHIPPED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(shipped));

        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(1L, "CANCELLED"));
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }
}
