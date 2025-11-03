package com.example.Kafka_Fault_Tolerant.repository;

import com.example.Kafka_Fault_Tolerant.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order,Long> {
}
