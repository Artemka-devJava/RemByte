package com.rembyte.repository;

import com.rembyte.model.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findByService_Id(Long serviceId);
    long countByOrder_Id(Long orderId);
}
