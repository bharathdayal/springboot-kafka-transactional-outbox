package com.example.Kafka_Fault_Tolerant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity(name="outbox")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;   // e.g., "Order"
    private String aggregateId;     // orderId
    private String type;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String payload;         // JSON payload

    private boolean published = false;

    private Instant createdAt = Instant.now();

}
