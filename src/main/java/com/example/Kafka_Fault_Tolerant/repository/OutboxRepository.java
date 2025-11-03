package com.example.Kafka_Fault_Tolerant.repository;

import com.example.Kafka_Fault_Tolerant.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent,String> {

    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
