package com.example.Kafka_Fault_Tolerant.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.Collection;

@Configuration
public class ContainerDebugConfig {

    @Bean
    public ApplicationRunner checkContainers(KafkaListenerEndpointRegistry registry) {
        return args -> {
            System.out.println("=== Container wiring check ===");
            for (MessageListenerContainer c : registry.getListenerContainers()) {
                System.out.println("Container listenerId=" + c.getListenerId()
                        + " | running=" + c.isRunning()
                        + " | commonErrorHandler=" + safe(() -> c.getContainerProperties()));
            }
        };
    }

    private String safe(ThrowableSupplier s) {
        try { Object o = s.get(); return o == null ? "null" : o.getClass().getName(); }
        catch (Throwable t) { return "(error reading)"; }
    }

    private interface ThrowableSupplier { Object get() throws Exception; }

    @Bean
    public ApplicationRunner inspectKafkaRegistry(ApplicationContext ctx) {
        return args -> {
            System.out.println("=== Kafka registry inspection ===");

            String[] registryNames = ctx.getBeanNamesForType(KafkaListenerEndpointRegistry.class);
            System.out.println("KafkaListenerEndpointRegistry bean names (by type):");
            for (String n : registryNames) System.out.println("  - " + n);
            if (registryNames.length == 0) {
                System.out.println("  -> No registry beans found by type");
            }

            // Check the default bean name specifically
            System.out.println("containsBean('kafkaListenerEndpointRegistry') = " + ctx.containsBean("kafkaListenerEndpointRegistry"));

            // Iterate registries and print containers
            for (String name : registryNames) {
                try {
                    System.out.println("Inspecting registry bean: " + name);
                    KafkaListenerEndpointRegistry registry = ctx.getBean(name, KafkaListenerEndpointRegistry.class);
                    Collection<MessageListenerContainer> containers = registry.getListenerContainers();
                    System.out.println("  listenerContainers.size = " + (containers == null ? "null" : containers.size()));
                    if (containers != null) {
                        for (MessageListenerContainer c : containers) {
                            String id = "(unknown)";
                            try {
                                id = c.getListenerId();
                            } catch (Exception ignore) { }
                            System.out.println("    Listener ID: " + id
                                    + " | class: " + c.getClass().getSimpleName()
                                    + " | running: " + c.isRunning());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("  Failed to inspect registry " + name + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Show whether the post-processor exists and its bean names
            String[] postProcessorNames = ctx.getBeanNamesForType(KafkaListenerAnnotationBeanPostProcessor.class);
            System.out.println("KafkaListenerAnnotationBeanPostProcessor beans:");
            for (String n : postProcessorNames) System.out.println("  - " + n);
            if (postProcessorNames.length == 0) {
                System.out.println("  -> No KafkaListenerAnnotationBeanPostProcessor beans present");
            }

            // For extra visibility, list beans that contain 'Listener' in their name (helpful)
            System.out.println("Beans with 'Listener' in name (quick scan):");
            for (String n : ctx.getBeanDefinitionNames()) {
                if (n.toLowerCase().contains("listener")) System.out.println("  - " + n);
            }

            System.out.println("=== end inspection ===");
        };
    }

    @Bean
    public ApplicationRunner kafkaInfraRunner(ApplicationContext ctx) {
        return args -> {
            System.out.println("=== Kafka infra diagnostic ===");
            System.out.println("Has bean KafkaListenerEndpointRegistry: " + ctx.containsBean("kafkaListenerEndpointRegistry"));
            System.out.println("Has bean kafkaListenerAnnotationBeanPostProcessor: " +
                    (ctx.getBeanNamesForType(KafkaListenerAnnotationBeanPostProcessor.class).length > 0));
            System.out.println("Has bean org.springframework.kafka.listener.KafkaListenerEndpointRegistry: " +
                    (ctx.getBeanNamesForType(KafkaListenerEndpointRegistry.class).length > 0));
            System.out.println("Beans of type ConcurrentKafkaListenerContainerFactory: ");
            for (String n : ctx.getBeanNamesForType(ConcurrentKafkaListenerContainerFactory.class)) {
                System.out.println(" - " + n);
            }
        };
    }

    @Bean
    public ApplicationRunner printListenerContainers(ApplicationContext ctx) {
        return args -> {
            System.out.println("=== Kafka Listener Containers at startup ===");
            if (ctx.containsBean("kafkaListenerEndpointRegistry")) {
                KafkaListenerEndpointRegistry registry =
                        ctx.getBean("kafkaListenerEndpointRegistry", KafkaListenerEndpointRegistry.class);
                for (MessageListenerContainer container : registry.getListenerContainers()) {
                    System.out.println("Listener id: " + container.getListenerId()
                            + " | class: " + container.getClass().getName()
                            + " | isRunning: " + container.isRunning());
                    try {
                        Object handler = container.getContainerProperties();
                        System.out.println("  commonErrorHandler: " + (handler == null ? "null" : handler.getClass().getName()));
                    } catch (Exception e) {
                        System.out.println("  could not read commonErrorHandler: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("No kafkaListenerEndpointRegistry bean found");
            }
        };
       }


}


