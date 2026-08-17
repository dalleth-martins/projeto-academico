package com.tcc.accountservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDTO{

        @NotBlank(message = "CPF é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos numéricos")
        String documento;

        String nome;

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email;

        @NotBlank(message = "Telefone é obrigatório")
        String telefone;

        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate dataNascimento;
}
