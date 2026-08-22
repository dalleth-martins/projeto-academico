package com.tcc.accountservice.service;

import com.tcc.accountservice.config.event.AccountCreatedEvent;
import com.tcc.accountservice.config.event.AccountEventPublisher;
import com.tcc.accountservice.dto.request.AccountRequestDTO;
import com.tcc.accountservice.dto.request.DebitRequestDTO;
import com.tcc.accountservice.dto.response.AccountResponseDTO;
import com.tcc.accountservice.dto.response.DebitResponseDTO;
import com.tcc.accountservice.entidade.Account;
import com.tcc.accountservice.entidade.AccountOperation;
import com.tcc.accountservice.enums.AccountStatus;
import com.tcc.accountservice.enums.AccountType;
import com.tcc.accountservice.enums.DebitOperationStatus;
import com.tcc.accountservice.exception.AccountNotFoundException;
import com.tcc.accountservice.exception.CustomerNotFoundException;
import com.tcc.accountservice.exception.SaldoInsuficienteException;
import com.tcc.accountservice.repository.AccountDebitRepository;
import com.tcc.accountservice.repository.AccountRepository;
import com.tcc.accountservice.repository.DebitOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String AGENCIA_PADRAO = "0001";
    private final SecureRandom random = new SecureRandom();

    private final AccountRepository accountRepository;
    private final CustomerService customerService;
    private final AccountEventPublisher eventPublisher;
    private final DebitOperationRepository debitOperationRepository;
    private final AccountDebitRepository accountDebitRepository;

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

        eventPublisher.publishAccountCreated(AccountCreatedEvent.builder()
                .accountId(salvo.getId())
                .clienteId(salvo.getClienteId())
                .numeroConta(salvo.getNumeroConta())
                .agencia(salvo.getAgencia())
                .tipo(salvo.getTipo().name())
                .status(salvo.getStatus().name())
                .saldo(salvo.getSaldo())
                .criadoEm(salvo.getCriadoEm())
                .build());

        return toResponseDTO(salvo);
    }

    public AccountResponseDTO buscarPorId(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return toResponseDTO(account);
    }

    public DebitResponseDTO debitar(String contaId, DebitRequestDTO request) {

        AccountOperation operacao = AccountOperation.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .contaId(contaId)
                .valor(request.getValor())
                .status(DebitOperationStatus.PROCESSANDO)
                .processadoEm(LocalDateTime.now())
                .build();

        try {
            debitOperationRepository.insert(operacao);
        } catch (DuplicateKeyException e) {
            log.info("idempotencyKey já registrada, retornando resultado existente. key={}", request.getIdempotencyKey());
            return toDebitResponseDTO(
                    debitOperationRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElseThrow()
            );
        }

        Account debitado = accountDebitRepository
                .debitarSeSaldoSuficiente(contaId, request.getValor())
                .orElse(null);

        if (debitado == null) {
            boolean contaExiste = accountRepository.existsById(contaId);
            operacao.setStatus(contaExiste
                    ? DebitOperationStatus.FALHOU_SALDO_INSUFICIENTE
                    : DebitOperationStatus.FALHOU_CONTA_INVALIDA);
            debitOperationRepository.save(operacao);

            if (!contaExiste) {
                throw new AccountNotFoundException(contaId);
            }
            throw new SaldoInsuficienteException(contaId);
        }

        operacao.setSaldoApos(debitado.getSaldo());
        operacao.setStatus(DebitOperationStatus.CONCLUIDO);
        debitOperationRepository.save(operacao);

        return toDebitResponseDTO(operacao);
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

    private DebitResponseDTO toDebitResponseDTO(AccountOperation operacao) {
        return DebitResponseDTO.builder()
                .contaId(operacao.getContaId())
                .valor(operacao.getValor())
                .saldoApos(operacao.getSaldoApos())
                .idempotencyKey(operacao.getIdempotencyKey())
                .status(operacao.getStatus())
                .processadoEm(operacao.getProcessadoEm())
                .build();
    }

}