package com.example.Kafka_Fault_Tolerant.component;

import com.example.Kafka_Fault_Tolerant.dto.OrderEvent;
import com.example.Kafka_Fault_Tolerant.model.OutboxEvent;
import com.example.Kafka_Fault_Tolerant.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderListener {

    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    public OrderListener(ObjectMapper objectMapper,OutboxRepository outboxRepository) {
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }



    @KafkaListener(topics = "orders", groupId = "orders-service", containerFactory = "kafkaListenerContainerFactory")
    //@RetryableTopic(attempts = "5", backoff = @Backoff(delay = 2000, multiplier = 2))
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String payload = record.value();
        System.out.println("PAYLOAD ===> " + payload);

        OrderEvent incomingEvent;

        try {
            incomingEvent = objectMapper.readValue(payload, OrderEvent.class);
        } catch (Exception ex) {
            System.err.println("Invalid payload, sending to DLQ: " + ex.getMessage());
            throw new RuntimeException(ex); // let error handler DLQ it
        }

        String orderId = incomingEvent.orderId();

        try {
            // 1. Read the latest outbox record for this aggregateId
            OutboxEvent latest = outboxRepository
                    .findTopByAggregateIdOrderByIdDesc(orderId)
                    .orElse(null);

            if (latest != null) {
                OrderEvent authoritativeEvent = objectMapper.readValue(latest.getPayload(), OrderEvent.class);

                // 2. If updated Outbox says forceDLQ = false → ACK and SKIP
                if (!authoritativeEvent.forceDLQ()) {
                    System.out.println("Skipping: latest OutboxEvent says forceDLQ=false for orderId=" + orderId);
                    ack.acknowledge();
                    return;
                }

                // 3. ELSE → force DLQ
                System.out.println("forceDLQ=true sending to retry/DLQ for orderId=" + orderId);
                throw new RuntimeException("Forced DLQ for " + orderId);
            }

            // No Outbox found → safe fallback: ACK
            System.out.println("No OutboxEvent found. Acknowledging safely.");
            ack.acknowledge();

        } catch (Exception ex) {
            System.err.println("Error while evaluating outbox: " + ex.getMessage());
            throw new RuntimeException(ex); // → error handler will retry/DLQ
        }
    }


}
