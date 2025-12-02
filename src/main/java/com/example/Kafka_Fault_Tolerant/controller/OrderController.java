package com.example.Kafka_Fault_Tolerant.controller;

import com.example.Kafka_Fault_Tolerant.dto.OrderEvent;
import com.example.Kafka_Fault_Tolerant.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service=service;
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody OrderEvent event) throws JsonProcessingException {
        service.CreateOrderAndPublish(event);
        return  ResponseEntity.accepted().body(event.orderId());
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable("id") String id, @RequestBody OrderEvent event) throws JsonProcessingException{
                if(!id.equals(event.orderId())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Path id and orderId must match");
                }
                 service.UpdateOrderAndPublish(event);
                return ResponseEntity.accepted().body(event.orderId());
    }
}
