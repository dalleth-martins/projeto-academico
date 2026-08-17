package com.tcc.accountservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tcc.accountservice.enums.AccountStatus;
import com.tcc.accountservice.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponseDTO {
    String id;
    String clienteId;
    String numeroConta;
    String agencia;
    AccountType tipo;
    AccountStatus status;
    BigDecimal saldo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime criadoEm;

}
