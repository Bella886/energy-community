package com.energy.user.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    public static final String ENERGY_QUEUE = "energy_queue";
    
    @Bean
    public Queue energyQueue() {
        return new Queue(ENERGY_QUEUE, true);
    }
} 