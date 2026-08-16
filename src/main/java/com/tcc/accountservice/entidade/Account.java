package com.tcc.accountservice.entidade;

import com.tcc.accountservice.enums.AccountStatus;
import com.tcc.accountservice.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;

    private String clienteId;

    @Indexed(unique = true)
    private String numeroConta;

    private String agencia;

    private AccountType tipo;

    private AccountStatus status;

    private BigDecimal saldo;

    @CreatedDate
    private LocalDateTime criadoEm;
}
