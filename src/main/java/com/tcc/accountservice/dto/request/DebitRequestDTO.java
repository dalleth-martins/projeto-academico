package com.tcc.accountservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DebitRequestDTO {
    @NotNull(message = "valor é obrigatório")
    @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
    BigDecimal valor;

    @NotBlank(message = "idempotencyKey é obrigatório")
    String idempotencyKey;
}
