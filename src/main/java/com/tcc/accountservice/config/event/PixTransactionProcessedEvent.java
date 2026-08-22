package com.tcc.accountservice.config.event;


import com.tcc.accountservice.enums.TransactionStatus;

public class PixTransactionProcessedEvent {
    private String transactionId;
    private String idempotencyKey;
    private TransactionStatus status;
    private String message;
}
