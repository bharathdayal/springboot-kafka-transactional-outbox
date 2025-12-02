package com.example.Kafka_Fault_Tolerant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name="failed_event")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // renamed from 'topic' -> safe name kept as topic
    @Column(name = "topic", length = 255)
    private String topic;

    // reserve word avoided: 'partition' -> 'partition_no'
    @Column(name = "partition_no")
    private Integer partitionNo;

    // avoid 'key' column name (reserved-ish) -> 'record_key'
    @Column(name = "record_key", length = 512)
    private String recordKey;

    @Lob
    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
