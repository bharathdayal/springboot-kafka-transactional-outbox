package com.example.Kafka_Fault_Tolerant.repository;

import com.example.Kafka_Fault_Tolerant.model.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent,Long> {
}
