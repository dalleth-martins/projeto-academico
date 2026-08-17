package com.tcc.accountservice.config.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.tcc.accountservice.config.RabbitMQConfig.ACCOUNT_CREATED_ROUTING_KEY;
import static com.tcc.accountservice.config.RabbitMQConfig.ACCOUNT_EXCHANGE;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishAccountCreated(AccountCreatedEvent event) {

        log.info("Publicando evento AccountCreated para accountId={}", event.accountId);

        rabbitTemplate.convertAndSend(ACCOUNT_EXCHANGE, ACCOUNT_CREATED_ROUTING_KEY, event);
    }
}
