package com.example.Kafka_Fault_Tolerant.dto;


import java.util.UUID;

public record OrderEvent(String orderId, String product, int qty,boolean forceDLQ) {

    public OrderEvent(String product, int qty,boolean forceDLQ) {

        this(UUID.randomUUID().toString(),product,qty,forceDLQ);

    }

}
