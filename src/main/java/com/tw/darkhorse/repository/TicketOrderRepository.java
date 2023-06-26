package com.tw.darkhorse.repository;

import com.tw.darkhorse.entity.TicketOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketOrderRepository extends JpaRepository<TicketOrderEntity, Long> {
    List<TicketOrderEntity> findByUserId(Long userId);
}
