package com.tcc.accountservice.config.messaging;

import com.tcc.accountservice.config.event.PixTransactionRequestedEvent;
import com.tcc.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PixTransactionListener {
    private final AccountService accountService;

    @RabbitListener(queues = "pix.transaction.requested")
    public void processar(PixTransactionRequestedEvent event) {

        accountService.processarTransacao(event);
    }
}
