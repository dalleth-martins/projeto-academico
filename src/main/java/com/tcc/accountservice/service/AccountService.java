package com.tcc.accountservice.service;

import com.tcc.accountservice.dto.AccountRequestDTO;
import com.tcc.accountservice.dto.AccountResponseDTO;
import com.tcc.accountservice.entidade.Account;
import com.tcc.accountservice.enums.AccountStatus;
import com.tcc.accountservice.enums.AccountType;
import com.tcc.accountservice.exception.AccountNotFoundException;
import com.tcc.accountservice.exception.CustomerNotFoundException;
import com.tcc.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AccountService {
    private static final String AGENCIA_PADRAO = "0001";
    private final SecureRandom random = new SecureRandom();

    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    public AccountResponseDTO criar(AccountRequestDTO request) {
        if (!customerService.existePorId(request.getClienteId())) {
            throw new CustomerNotFoundException(request.getClienteId());
        }

        Account account = Account.builder()
                .clienteId(request.getClienteId())
                .numeroConta(gerarNumeroContaUnico())
                .agencia(AGENCIA_PADRAO)
                .tipo(AccountType.CORRENTE)
                .status(AccountStatus.ATIVA)
                .saldo(BigDecimal.ZERO)
                .build();

        Account salvo = accountRepository.save(account);
        return toResponseDTO(salvo);
    }

    public AccountResponseDTO buscarPorId(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return toResponseDTO(account);
    }

    private String gerarNumeroContaUnico() {
        String numero;
        do {
            numero = String.valueOf(100000 + random.nextInt(900000));
        } while (accountRepository.existsByNumeroConta(numero));
        return numero;
    }

    private AccountResponseDTO toResponseDTO(Account account) {
        return new AccountResponseDTO(
                account.getId(),
                account.getClienteId(),
                account.getNumeroConta(),
                account.getAgencia(),
                account.getTipo(),
                account.getStatus(),
                account.getSaldo(),
                account.getCriadoEm()
        );
    }
}
