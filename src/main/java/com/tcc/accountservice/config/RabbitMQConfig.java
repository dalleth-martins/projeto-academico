package com.tcc.accountservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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

    // --- Dead-letter: para onde vão mensagens que estouraram o limite de
    // retry configurado em application.yaml (spring.rabbitmq.listener.simple.retry).
    // Isso evita que um erro técnico (Mongo fora do ar, bug, etc.) faça a
    // mesma mensagem ser reentregue para sempre.
    public static final String PIX_TRANSACTION_DLX =
            "pix.transaction.dlx";

    public static final String PIX_TRANSACTION_REQUESTED_DLQ =
            "pix.transaction.requested.dlq";

    public static final String PIX_TRANSACTION_DLQ_ROUTING_KEY =
            "pix.transaction.requested.dlq";


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
    public DirectExchange pixTransactionDlx() {
        return new DirectExchange(PIX_TRANSACTION_DLX, true, false);
    }

    @Bean
    public Queue pixTransactionRequestedQueue() {
        return QueueBuilder.durable(PIX_TRANSACTION_REQUESTED_QUEUE)
                .withArgument("x-dead-letter-exchange", PIX_TRANSACTION_DLX)
                .withArgument("x-dead-letter-routing-key", PIX_TRANSACTION_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue pixTransactionRequestedDlq() {
        return QueueBuilder.durable(PIX_TRANSACTION_REQUESTED_DLQ).build();
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
    public Binding pixTransactionRequestedDlqBinding(
            Queue pixTransactionRequestedDlq,
            DirectExchange pixTransactionDlx
    ) {
        return BindingBuilder
                .bind(pixTransactionRequestedDlq)
                .to(pixTransactionDlx)
                .with(PIX_TRANSACTION_DLQ_ROUTING_KEY);
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

        // propaga o contexto do trace (traceparent) nos headers das mensagens
        template.setObservationEnabled(true);

        return template;
    }
}