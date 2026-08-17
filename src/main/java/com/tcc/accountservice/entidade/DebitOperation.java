package com.tcc.accountservice.entidade;

import com.tcc.accountservice.enums.DebitOperationStatus;
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
@Document(collection = "debit_operations")
public class DebitOperation {
    @Id
    private String id;

    @Indexed(unique = true)
    private String idempotencyKey;

    private String contaId;

    private BigDecimal valor;

    private BigDecimal saldoApos;

    private DebitOperationStatus status;

    private LocalDateTime processadoEm;
}
