package com.example.Kafka_Fault_Tolerant.component;

import com.example.Kafka_Fault_Tolerant.model.OutboxEvent;
import com.example.Kafka_Fault_Tolerant.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate; // sending raw JSON
    private final ObjectMapper mapper;

    public OutboxPublisher(OutboxRepository outboxRepository, @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    /**
     * Poll and publish pending outbox events.
     * Use small batches and mark published after success.
     * This method is resilient: on failure, event remains unpublished and will be retried.
     */
    @Scheduled(fixedDelay = 2000)
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent oe : pending) {
            try {
                // publish within a Kafka transaction so the write to Kafka is atomic on the Kafka side
                kafkaTemplate.executeInTransaction(k -> {
                    try {
                        k.send("orders", oe.getAggregateId(), oe.getPayload()).get(); // block until sent
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                });

                // mark published in DB (no transaction here; small window is okay since publisher runs again if marking fails)
                markPublished(oe.getId());
            } catch (Exception ex) {
                // log & continue - will retry on next scheduled run
                System.err.println("Failed to publish outbox id=" + oe.getId() + " error=" + ex.getMessage());
            }
        }
    }

    @Transactional
    protected void markPublished(Long id) {
        OutboxEvent oe = outboxRepository.findById(String.valueOf(id)).orElse(null);
        if (oe != null) {
            oe.setPublished(true);
            outboxRepository.save(oe);
        }
    }
}
