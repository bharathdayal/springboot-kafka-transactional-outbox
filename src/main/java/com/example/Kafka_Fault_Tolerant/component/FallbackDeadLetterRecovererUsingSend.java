package com.example.Kafka_Fault_Tolerant.component;

import com.example.Kafka_Fault_Tolerant.model.FailedEvent;
import com.example.Kafka_Fault_Tolerant.repository.FailedEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FallbackDeadLetterRecovererUsingSend implements ConsumerRecordRecoverer {

    private final DeadLetterPublishingRecoverer kafkaRecoverer;
    private final KafkaTemplate<String, String> nonTxKafkaTemplate;
    private final FailedEventRepository failedEventRepository;

    public FallbackDeadLetterRecoverer(DeadLetterPublishingRecoverer kafkaRecoverer,
                                       KafkaTemplate<String, String> nonTxKafkaTemplate,
                                       FailedEventRepository failedEventRepository) {
        this.kafkaRecoverer = Objects.requireNonNull(kafkaRecoverer, "kafkaRecoverer must not be null");
        this.nonTxKafkaTemplate = Objects.requireNonNull(nonTxKafkaTemplate, "nonTxKafkaTemplate must not be null");
        this.failedEventRepository = Objects.requireNonNull(failedEventRepository, "failedEventRepository must not be null");
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception cause) {
        String origTopic = record.topic();
        int origPartition = record.partition();
        Object key = record.key();
        Object value = record.value();

        String keyStr = key == null ? null : key.toString();
        String valueStr = value == null ? null : value.toString();

        System.out.println("[FallbackRecoverer] invoked for topic=" + origTopic
                + " partition=" + origPartition + " key=" + keyStr
                + " cause=" + (cause == null ? "null" : cause.getMessage()));

        // IMPORTANT: choose the same DLT naming convention you used when creating topics.
        // Your config used "orders.DLT" originally. If you use "orders-dlt" anywhere, be consistent.
        String dltTopic = origTopic + "-dlt"; // adjust if you use hyphen style: origTopic + "-dlt"

        // Try the safe, explicit send path (avoid relying on kafkaRecoverer which may throw)
        try {
            // 1) get partition info for DLT topic
            List<PartitionInfo> dltPartitions = nonTxKafkaTemplate.partitionsFor(dltTopic);
            int dltPartitionCount = (dltPartitions == null || dltPartitions.isEmpty()) ? 0 : dltPartitions.size();

            ProducerRecord<String, String> producerRecord;
            if (dltPartitionCount > 0) {
                // map original partition to a valid dlt partition
                int targetPartition = Math.floorMod(origPartition, dltPartitionCount);
                producerRecord = new ProducerRecord<>(dltTopic, targetPartition, keyStr, valueStr);
                System.out.println("[FallbackRecoverer] sending to DLT partition " + targetPartition + "/" + dltPartitionCount);
            } else {
                // metadata not available yet -> send without partition (broker will pick)
                producerRecord = new ProducerRecord<>(dltTopic, keyStr, valueStr);
                System.out.println("[FallbackRecoverer] DLT partition metadata unavailable; sending without partition");
            }

            // 2) send via non-tx KafkaTemplate and block to obtain metadata
            CompletableFuture<SendResult<String, String>> future = nonTxKafkaTemplate.send(producerRecord);
            SendResult<String, String> sendResult = future.get(); // may throw
            RecordMetadata rm = sendResult.getRecordMetadata();

            System.out.println("[FallbackRecoverer] published to DLT successfully: topic="
                    + rm.topic() + " partition=" + rm.partition() + " offset=" + rm.offset());

            // 3) persist original failed event with DLT metadata
            persistDLTMetadata(origTopic, origPartition, keyStr, valueStr, cause, rm);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("[FallbackRecoverer] interrupted while publishing to DLT: " + ie.getMessage());
            persistFallback(record, cause, ie);
        } catch (Exception pubEx) {
            // Publish failed (could be TimeoutException for bad partition, or other KafkaException)
            System.err.println("[FallbackRecoverer] publish to DLT failed: " + pubEx.getMessage());
            pubEx.printStackTrace();

            // As a last resort persist the failed record and both the original cause and publish exception
            persistFallback(record, cause, pubEx);
        }
    }



    private void persistDLTMetadata(String origTopic,
                                    int origPartition,
                                    String keyStr,
                                    String valueStr,
                                    Exception cause,
                                    RecordMetadata rm) {
        try {
            FailedEvent fe = new FailedEvent();
            fe.setTopic(origTopic);
            fe.setPartitionNo(origPartition);
            fe.setRecordKey(keyStr);
            fe.setPayload(valueStr);
            fe.setErrorMessage(cause == null ? null : cause.toString());

            fe.setTopic(rm.topic());
            fe.setPartitionNo(rm.partition());

            FailedEvent saved = failedEventRepository.save(fe);
            System.out.println("[FallbackRecoverer] persisted FailedEvent id=" + saved.getId() + " with DLT metadata");
        } catch (Exception dbEx) {
            System.err.println("[FallbackRecoverer] failed to persist DLT metadata: " + dbEx.getMessage());
            dbEx.printStackTrace();
        }
    }

    private void persistFallback(ConsumerRecord<?, ?> record, Exception originalCause, Exception publishException) {
        try {
            FailedEvent fe = new FailedEvent();
            fe.setTopic(record.topic());
            fe.setPartitionNo(record.partition());
            fe.setRecordKey(record.key() == null ? null : String.valueOf(record.key()));
            fe.setPayload(record.value() == null ? null : String.valueOf(record.value()));

            String combinedError = "OriginalCause: " + (originalCause == null ? "null" : originalCause.toString())
                    + "\nPublishException: " + (publishException == null ? "null" : publishException.toString());
            fe.setErrorMessage(combinedError);

            FailedEvent saved = failedEventRepository.save(fe);
            System.out.println("[FallbackRecoverer] persisted FAILED FailedEvent id=" + saved.getId());
        } catch (Exception dbEx) {
            System.err.println("[FallbackRecoverer] failed to persist FailedEvent: " + dbEx.getMessage());
            dbEx.printStackTrace();
        }
    }
}
