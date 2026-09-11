package com.rembyte.repository;

import com.rembyte.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByDoneFalseOrderByDueAtAscIdAsc();
    List<Reminder> findByOrderIdOrderByIdDesc(Long orderId);
    long deleteByOrderId(Long orderId);
    long deleteByClientId(Long clientId);
}
