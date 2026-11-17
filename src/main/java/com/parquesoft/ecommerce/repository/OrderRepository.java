package com.parquesoft.ecommerce.repository;

import com.parquesoft.ecommerce.entity.OrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByCustomerId(Long customerId);

    List<OrderEntity> findByStatus(String status);

    @Query("SELECT o FROM OrderEntity o WHERE o.totalAmount >= :minAmount")
    List<OrderEntity> findOrdersAboveAmount(@Param("minAmount") double minAmount);
}
