package com.tcc.accountservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ACCOUNT_EXCHANGE = "account.exchange";

    public static final String ACCOUNT_CREATED_ROUTING_KEY =
            "account.created";


    public static final String PIX_TRANSACTION_EXCHANGE =
            "pix.transaction.exchange";

    public static final String PIX_TRANSACTION_REQUESTED_ROUTING_KEY =
            "pix.transaction.requested";

    public static final String PIX_TRANSACTION_PROCESSED_ROUTING_KEY =
            "pix.transaction.processed";

    public static final String PIX_TRANSACTION_REQUESTED_QUEUE =
            "pix.transaction.requested.queue";


    @Bean
    public TopicExchange accountExchange() {
        return new TopicExchange(
                ACCOUNT_EXCHANGE, true, false
        );
    }

    @Bean
    public TopicExchange pixTransactionExchange() {
        return new TopicExchange(PIX_TRANSACTION_EXCHANGE, true,false
        );
    }

    @Bean
    public Queue pixTransactionRequestedQueue() {
        return new Queue(PIX_TRANSACTION_REQUESTED_QUEUE, true
        );
    }

    @Bean
    public Binding pixTransactionRequestedBinding(
            Queue pixTransactionRequestedQueue,
            TopicExchange pixTransactionExchange
    ) {
        return BindingBuilder
                .bind(pixTransactionRequestedQueue)
                .to(pixTransactionExchange)
                .with(PIX_TRANSACTION_REQUESTED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter converter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        template.setMessageConverter(converter);

        return template;
    }
}