package com.tcc.accountservice.service;

import com.tcc.accountservice.config.event.AccountCreatedEvent;
import com.tcc.accountservice.config.event.AccountEventPublisher;
import com.tcc.accountservice.config.event.PixTransactionProcessedEvent;
import com.tcc.accountservice.config.event.PixTransactionRequestedEvent;
import com.tcc.accountservice.dto.request.AccountRequestDTO;
import com.tcc.accountservice.dto.request.DebitRequestDTO;
import com.tcc.accountservice.dto.response.AccountResponseDTO;
import com.tcc.accountservice.dto.response.DebitResponseDTO;
import com.tcc.accountservice.entidade.Account;
import com.tcc.accountservice.entidade.AccountOperation;
import com.tcc.accountservice.enums.AccountOperationStatus;
import com.tcc.accountservice.enums.AccountStatus;
import com.tcc.accountservice.enums.AccountType;
import com.tcc.accountservice.enums.OperationType;
import com.tcc.accountservice.exception.AccountNotFoundException;
import com.tcc.accountservice.exception.CustomerNotFoundException;
import com.tcc.accountservice.exception.SaldoInsuficienteException;
import com.tcc.accountservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AccountOperationRepository accountOperationRepository;
    private final AccountCreditRepository accountCreditRepository;


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

        eventPublisher.publishAccountCreated(
                AccountCreatedEvent.builder()
                        .accountId(salvo.getId())
                        .clienteId(salvo.getClienteId())
                        .numeroConta(salvo.getNumeroConta())
                        .agencia(salvo.getAgencia())
                        .tipo(salvo.getTipo().name())
                        .status(salvo.getStatus().name())
                        .saldo(salvo.getSaldo())
                        .criadoEm(salvo.getCriadoEm())
                        .build()
        );

        return toResponseDTO(salvo);
    }


    public AccountResponseDTO buscarPorId(String id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        return toResponseDTO(account);
    }


    public DebitResponseDTO debitar(
            String contaId,
            DebitRequestDTO request
    ) {

        AccountOperation operacao = AccountOperation.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .accountId(contaId)
                .amount(request.getValor())
                .status(AccountOperationStatus.PROCESSANDO)
                .processedAt(LocalDateTime.now())
                .build();

        try {

            debitOperationRepository.insert(operacao);

        } catch (DuplicateKeyException e) {

            log.info(
                    "idempotencyKey já registrada, retornando resultado existente. key={}",
                    request.getIdempotencyKey()
            );

            return toDebitResponseDTO(
                    debitOperationRepository
                            .findByIdempotencyKey(
                                    request.getIdempotencyKey()
                            )
                            .orElseThrow()
            );
        }

        Account debitado = accountDebitRepository
                .debitarSeSaldoSuficiente(
                        contaId,
                        request.getValor()
                )
                .orElse(null);

        if (debitado == null) {

            boolean contaExiste = accountRepository.existsById(contaId);

            operacao.setStatus(
                    contaExiste
                            ? AccountOperationStatus.FALHOU_SALDO_INSUFICIENTE
                            : AccountOperationStatus.FALHOU_CONTA_INVALIDA
            );

            debitOperationRepository.save(operacao);

            if (!contaExiste) {
                throw new AccountNotFoundException(contaId);
            }

            throw new SaldoInsuficienteException(contaId);
        }

        operacao.setAmount(debitado.getSaldo());
        operacao.setStatus(AccountOperationStatus.CONCLUIDO);

        debitOperationRepository.save(operacao);

        return toDebitResponseDTO(operacao);
    }


    private String gerarNumeroContaUnico() {

        String numero;

        do {
            numero = String.valueOf(
                    100000 + random.nextInt(900000)
            );

        } while (accountRepository.existsByNumeroConta(numero));

        return numero;
    }


    @Transactional
    public void processarTransacao(
            PixTransactionRequestedEvent event
    ) {

        log.info(
                "Iniciando processamento da transação Pix. transactionId={}",
                event.getTransactionId()
        );

        AccountOperation operacao = AccountOperation.builder()
                .transactionId(event.getTransactionId())
                .idempotencyKey(event.getIdempotencyKey())
                .accountId(event.getSourceAccountId())
                .type(OperationType.DEBIT)
                .amount(event.getAmount())
                .status(AccountOperationStatus.PROCESSANDO)
                .processedAt(LocalDateTime.now())
                .build();

        try {

            accountOperationRepository.insert(operacao);

        } catch (DuplicateKeyException e) {

            log.info(
                    "Transação já processada. transactionId={} idempotencyKey={}",
                    event.getTransactionId(),
                    event.getIdempotencyKey()
            );

            AccountOperation existente =
                    accountOperationRepository
                            .findByIdempotencyKey(
                                    event.getIdempotencyKey()
                            )
                            .orElseThrow();

            publicarResultado(
                    event,
                    existente.getStatus()
            );

            return;
        }

        Account origem = accountDebitRepository
                .debitarSeSaldoSuficiente(
                        event.getSourceAccountId(),
                        event.getAmount()
                )
                .orElseThrow(() ->
                        new SaldoInsuficienteException(
                                event.getSourceAccountId()
                        )
                );

        Account destino = accountCreditRepository
                .creditar(
                        event.getDestinationAccountId(),
                        event.getAmount()
                )
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                event.getDestinationAccountId()
                        )
                );

        operacao.setStatus(
                AccountOperationStatus.CONCLUIDO
        );

        operacao.setProcessedAt(
                LocalDateTime.now()
        );

        accountOperationRepository.save(operacao);


        log.info(
                "Transação Pix concluída com sucesso. transactionId={}",
                event.getTransactionId()
        );

        publicarResultado(
                event,
                AccountOperationStatus.CONCLUIDO
        );
    }


    private void publicarResultado(
            PixTransactionRequestedEvent event,
            AccountOperationStatus status
    ) {

        String message = status == AccountOperationStatus.CONCLUIDO
                ? "Transação processada com sucesso"
                : "Transação não processada: " + status;

        eventPublisher.publishPixTransactionProcessed(
                PixTransactionProcessedEvent.builder()
                        .transactionId(event.getTransactionId())
                        .idempotencyKey(event.getIdempotencyKey())
                        .status(status)
                        .message(message)
                        .build()
        );
    }


    private AccountResponseDTO toResponseDTO(
            Account account
    ) {

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

    private DebitResponseDTO toDebitResponseDTO(
            AccountOperation operacao
    ) {

        return DebitResponseDTO.builder()
                .contaId(operacao.getAccountId())
                .valor(operacao.getAmount())
                .saldoApos(operacao.getAmount())
                .idempotencyKey(operacao.getIdempotencyKey())
                .status(operacao.getStatus())
                .processadoEm(operacao.getProcessedAt())
                .build();
    }
}