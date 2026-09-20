package com.tcc.accountservice.rabbitMq.event.messaging;

import com.tcc.accountservice.config.RabbitMQConfig;
import com.tcc.accountservice.metrics.AccountMetrics;
import com.tcc.accountservice.rabbitMq.event.PixTransactionRequestedEvent;
import com.tcc.accountservice.service.AccountService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PixTransactionListener {

    private final AccountService accountService;
    private final AccountMetrics accountMetrics;

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

        Timer.Sample amostra = accountMetrics.iniciarProcessamento();

        try {

            accountService.processarTransacao(event);

            accountMetrics.finalizarProcessamento(amostra, "sucesso");

        } catch (Exception e) {

            // "erro" = a mensagem vai ser reentregue (retry) ou parar na DLQ
            accountMetrics.finalizarProcessamento(amostra, "erro");

            log.error(
                    "Erro inesperado ao processar transação Pix. transactionId={}",
                    event.getTransactionId(), e
            );

            throw e;
        }
    }
}