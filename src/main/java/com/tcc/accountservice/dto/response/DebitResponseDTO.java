package com.tcc.accountservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tcc.accountservice.enums.AccountOperationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitResponseDTO {
    String contaId;
    BigDecimal valor;
    BigDecimal saldoApos;
    String idempotencyKey;
    AccountOperationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime processadoEm;
}
