package com.example.Kafka_Fault_Tolerant.repository;

import com.example.Kafka_Fault_Tolerant.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxEvent,String> {

    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
    Optional<OutboxEvent> findTopByAggregateIdOrderByIdDesc(String aggregateId);
}
