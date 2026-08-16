package com.tcc.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDTO {
    String id;
    String cpf;
    String nome;
    String email;
    String telefone;
    LocalDate dataNascimento;
    LocalDateTime criadoEm;
}
