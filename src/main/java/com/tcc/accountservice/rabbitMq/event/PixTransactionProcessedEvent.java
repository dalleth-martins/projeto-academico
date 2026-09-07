package com.tcc.accountservice.rabbitMq.event;


import com.tcc.accountservice.enums.AccountOperationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PixTransactionProcessedEvent {
    private String transactionId;
    private String idempotencyKey;
    private AccountOperationStatus status;
    private String message;
}
