package com.tcc.accountservice.rabbitMq.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PixTransactionRequestedEvent {
    private String transactionId;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String idempotencyKey;
}
