package com.tcc.accountservice.entidade;

import com.tcc.accountservice.enums.AccountOperationStatus;
import com.tcc.accountservice.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "account_operations")
public class AccountOperation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String idempotencyKey;

    private String transactionId;

    private String accountId;

    private String destinationAccountId;

    private OperationType type;

    private BigDecimal amount;

    private BigDecimal saldoApos;

    private AccountOperationStatus status;

    private LocalDateTime processedAt;
}