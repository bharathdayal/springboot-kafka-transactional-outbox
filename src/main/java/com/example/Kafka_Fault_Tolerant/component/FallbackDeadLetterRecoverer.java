package com.example.Kafka_Fault_Tolerant.component;

import com.example.Kafka_Fault_Tolerant.model.FailedEvent;
import com.example.Kafka_Fault_Tolerant.repository.FailedEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.util.Objects;

public class FallbackDeadLetterRecoverer implements ConsumerRecordRecoverer {

    private final DeadLetterPublishingRecoverer kafkaRecoverer;
    private final FailedEventRepository failedEventRepository;

    public FallbackDeadLetterRecoverer(DeadLetterPublishingRecoverer kafkaRecoverer,
                                       FailedEventRepository failedEventRepository) {
        this.kafkaRecoverer = Objects.requireNonNull(kafkaRecoverer);
        this.failedEventRepository = Objects.requireNonNull(failedEventRepository);
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception cause) {
        System.out.println("[FallbackRecoverer] invoked for topic=" + record.topic() + " partition=" + record.partition()
                + " key=" + record.key() + " cause=" + (cause == null ? "null" : cause.getMessage()));
        try {
            // Attempt to publish to Kafka DLT (this uses the non-tx KafkaTemplate in config)
            kafkaRecoverer.accept(record, cause);
            System.out.println("[FallbackRecoverer] published to DLT successfully.");
        } catch (Exception pubEx) {
            System.err.println("[FallbackRecoverer] publish to DLT failed: " + pubEx.getMessage());
            pubEx.printStackTrace();

            // Fallback: persist the failed record into DB for later replay
            try {
                FailedEvent fe = new FailedEvent();
                fe.setTopic(record.topic());
                fe.setPartitionNo(record.partition());
                fe.setRecordKey(record.key() == null ? null : String.valueOf(record.key()));
                fe.setPayload(record.value() == null ? null : String.valueOf(record.value()));

                String combinedError = "OriginalCause: " + (cause == null ? "null" : cause.toString())
                        + "\nPublishException: " + pubEx.toString();
                fe.setErrorMessage(combinedError);

                FailedEvent saved = failedEventRepository.save(fe);
                System.out.println("[FallbackRecoverer] persisted FailedEvent id=" + saved.getId());
            } catch (Exception dbEx) {
                System.err.println("[FallbackRecoverer] failed to persist FailedEvent: " + dbEx.getMessage());
                dbEx.printStackTrace();
            }
        }
    }
}
