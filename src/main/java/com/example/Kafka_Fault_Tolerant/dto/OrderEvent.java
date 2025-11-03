package com.example.Kafka_Fault_Tolerant.dto;


import java.util.UUID;

public record OrderEvent(String orderId, String product, int qty) {

    public OrderEvent(String product, int qty) {

        this(UUID.randomUUID().toString(),product,qty);

    }

}
