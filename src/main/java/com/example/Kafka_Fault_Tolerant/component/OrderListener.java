package com.example.Kafka_Fault_Tolerant.component;

import com.example.Kafka_Fault_Tolerant.dto.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderListener {

    private final ObjectMapper objectMapper;

    public OrderListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "orders", groupId = "orders-service", containerFactory = "kafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String payload = record.value();
        try {
            // convert JSON string to OrderEvent
            OrderEvent event = objectMapper.readValue(payload, OrderEvent.class);
            System.out.println("Processing order: " + event.orderId() + " product=" + event.product());
            ack.acknowledge();
        } catch (Exception ex) {
            // log and throw to trigger error handler / retry / DLT
            System.err.println("Failed to process payload: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

}
