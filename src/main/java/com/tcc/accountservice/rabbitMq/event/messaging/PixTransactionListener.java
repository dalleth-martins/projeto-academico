package com.tcc.accountservice.rabbitMq.event.messaging;

import com.tcc.accountservice.config.RabbitMQConfig;
import com.tcc.accountservice.rabbitMq.event.PixTransactionRequestedEvent;
import com.tcc.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PixTransactionListener {

    private final AccountService accountService;

    @RabbitListener(
            queues = RabbitMQConfig.PIX_TRANSACTION_REQUESTED_QUEUE
    )
    public void processar(
            PixTransactionRequestedEvent event
    ) {

        log.info(
                "Mensagem Pix recebida. transactionId={}",
                event.getTransactionId()
        );

        try {

            accountService.processarTransacao(event);

        } catch (Exception e) {

            log.error(
                    "Erro inesperado ao processar transação Pix. transactionId={}",
                    event.getTransactionId(), e
            );

            throw e;
        }
    }
}