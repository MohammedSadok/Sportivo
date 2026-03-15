package com.sadok.sportivo.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
  private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

  public static final String EXCHANGE_NAME = "sportivo.exchange";
  public static final String QUEUE_NAME = "sportivo.queue";
  public static final String ROUTING_KEY = "sportivo.notifications.#";

  @Bean
  public Queue queue() {
    logger.info("Creating Queue: {}", QUEUE_NAME);
    return new Queue(QUEUE_NAME, true);
  }

  @Bean
  public TopicExchange exchange() {
    logger.info("Creating TopicExchange: {}", EXCHANGE_NAME);
    return new TopicExchange(EXCHANGE_NAME);
  }

  @Bean
  public Binding binding(Queue queue, TopicExchange exchange) {
    logger.info("Creating Binding with routing key: {}", ROUTING_KEY);
    return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
  }
}
