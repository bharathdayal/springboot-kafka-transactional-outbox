package com.example.Kafka_Fault_Tolerant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.locks.ReentrantLock;

@SpringBootApplication
@EnableScheduling
public class KafkaFaultTolerantApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaFaultTolerantApplication.class, args);



		

	}

}
