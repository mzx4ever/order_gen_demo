package com.tw.darkhorse.repository;

import com.tw.darkhorse.entity.TicketOrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketOrderEventRepository extends JpaRepository<TicketOrderEventEntity, Long> {
    List<TicketOrderEventEntity> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
