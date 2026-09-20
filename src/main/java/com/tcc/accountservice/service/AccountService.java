package com.tcc.accountservice.service;

import com.tcc.accountservice.dto.request.AccountRequestDTO;
import com.tcc.accountservice.dto.request.BalanceResponseDTO;
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
import com.tcc.accountservice.metrics.AccountMetrics;
import com.tcc.accountservice.rabbitMq.event.AccountCreatedEvent;
import com.tcc.accountservice.rabbitMq.event.AccountEventPublisher;
import com.tcc.accountservice.rabbitMq.event.PixTransactionProcessedEvent;
import com.tcc.accountservice.rabbitMq.event.PixTransactionRequestedEvent;
import com.tcc.accountservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private final AccountDebitRepository accountDebitRepository;
    private final AccountOperationRepository accountOperationRepository;
    private final AccountCreditRepository accountCreditRepository;
    private final AccountMetrics accountMetrics;


    @Autowired
    @Lazy
    private AccountService self;

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

        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));

        return toResponseDTO(account);
    }

    public DebitResponseDTO debitar(String accountId, DebitRequestDTO request) {

        AccountOperation operacao = AccountOperation.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .accountId(accountId)
                .amount(request.getValor())
                .status(AccountOperationStatus.PROCESSANDO)
                .processedAt(LocalDateTime.now())
                .build();

        try {

            accountOperationRepository.insert(operacao);

        } catch (DuplicateKeyException e) {

            log.info("idempotencyKey já registrada, retornando resultado existente. key={}", request.getIdempotencyKey());

            return toDebitResponseDTO(accountOperationRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElseThrow());
        }

        Account debitado = accountDebitRepository.debitarSeSaldoSuficiente(accountId, request.getValor()).orElse(null);

        if (debitado == null) {

            boolean contaExiste = accountRepository.existsById(accountId);

            operacao.setStatus(contaExiste ? AccountOperationStatus.FALHOU_SALDO_INSUFICIENTE : AccountOperationStatus.FALHOU_CONTA_INVALIDA);

            operacao.setProcessedAt(LocalDateTime.now());

            accountOperationRepository.save(operacao);

            if (!contaExiste) {
                throw new AccountNotFoundException(accountId);
            }

            throw new SaldoInsuficienteException(accountId);
        }

        operacao.setSaldoApos(debitado.getSaldo());
        operacao.setStatus(AccountOperationStatus.CONCLUIDO);
        operacao.setProcessedAt(LocalDateTime.now());

        accountOperationRepository.save(operacao);

        return toDebitResponseDTO(operacao);
    }

    public AccountResponseDTO creditar(String accountId, BigDecimal valor) {

        Account account = accountCreditRepository
                .creditar(accountId, valor)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        return AccountResponseDTO.builder()
                .id(account.getId())
                .clienteId(account.getClienteId())
                .numeroConta(account.getNumeroConta())
                .agencia(account.getAgencia())
                .tipo(account.getTipo())
                .status(account.getStatus())
                .saldo(account.getSaldo())
                .criadoEm(account.getCriadoEm())
                .build();
    }

    public BalanceResponseDTO consultarSaldo(String accountId) {

        log.info("Buscando saldo da conta. accountId={}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("Conta não encontrada. accountId={}", accountId);
                    return new AccountNotFoundException(accountId);
                });

        log.info("Conta encontrada. accountId={} saldo={}",
                account.getId(), account.getSaldo());

        return BalanceResponseDTO.builder()
                .accountId(account.getId())
                .saldo(account.getSaldo())
                .build();
    }

    private String gerarNumeroContaUnico() {

        String numero;

        do {

            numero = String.valueOf(100000 + random.nextInt(900000));

        } while (accountRepository.existsByNumeroConta(numero));

        return numero;
    }

    public void processarTransacao(PixTransactionRequestedEvent event) {

        log.info("Iniciando processamento da transação Pix. transactionId={}", event.getTransactionId());

        AccountOperation existente = accountOperationRepository
                .findByIdempotencyKey(event.getIdempotencyKey())
                .orElse(null);

        if (existente != null) {

            log.info("Transação já processada. transactionId={} idempotencyKey={}",
                    event.getTransactionId(), event.getIdempotencyKey());

            publicarResultado(event, existente.getStatus());

            return;
        }

        AccountOperationStatus statusFinal;

        try {

            statusFinal = self.processarTransacaoAtomicamente(event);

        } catch (DuplicateKeyException e) {


            log.info("Operação já registrada por outra requisição concorrente. transactionId={} idempotencyKey={}",
                    event.getTransactionId(), event.getIdempotencyKey());

            statusFinal = accountOperationRepository.findByIdempotencyKey(event.getIdempotencyKey())
                    .map(AccountOperation::getStatus)
                    .orElse(AccountOperationStatus.FALHOU_CONTA_INVALIDA);

        } catch (AccountNotFoundException e) {

            log.warn("Transação revertida: conta inválida. transactionId={} motivo={}",
                    event.getTransactionId(), e.getMessage());

            statusFinal = AccountOperationStatus.FALHOU_CONTA_INVALIDA;
        }

        publicarResultado(event, statusFinal);
    }

    @Transactional
    public AccountOperationStatus processarTransacaoAtomicamente(PixTransactionRequestedEvent event) {

        AccountOperation operacao = AccountOperation.builder()
                .transactionId(event.getTransactionId())
                .idempotencyKey(event.getIdempotencyKey())
                .accountId(event.getSourceAccountId())
                .destinationAccountId(event.getDestinationAccountId())
                .type(OperationType.DEBIT)
                .amount(event.getAmount())
                .status(AccountOperationStatus.PROCESSANDO)
                .processedAt(LocalDateTime.now())
                .build();

        accountOperationRepository.insert(operacao);

        boolean destinoExiste = accountRepository.existsById(event.getDestinationAccountId());

        if (!destinoExiste) {
            return registrarFalha(operacao, event, AccountOperationStatus.FALHOU_CONTA_INVALIDA,
                    "Conta destino inválida");
        }

        Account origem = accountDebitRepository
                .debitarSeSaldoSuficiente(event.getSourceAccountId(), event.getAmount())
                .orElse(null);

        if (origem == null) {

            boolean contaOrigemExiste = accountRepository.existsById(event.getSourceAccountId());

            AccountOperationStatus status = contaOrigemExiste
                    ? AccountOperationStatus.FALHOU_SALDO_INSUFICIENTE
                    : AccountOperationStatus.FALHOU_CONTA_INVALIDA;

            return registrarFalha(operacao, event, status, "Débito na origem não realizado");
        }

        Account destino = accountCreditRepository
                .creditar(event.getDestinationAccountId(), event.getAmount())
                .orElse(null);

        if (destino == null) {

            log.error("Conta destino tornou-se inválida após débito na origem — revertendo. " +
                            "transactionId={} destinationAccountId={}",
                    event.getTransactionId(), event.getDestinationAccountId());
            throw new AccountNotFoundException(event.getDestinationAccountId());
        }

        operacao.setSaldoApos(origem.getSaldo());
        operacao.setStatus(AccountOperationStatus.CONCLUIDO);
        operacao.setProcessedAt(LocalDateTime.now());
        accountOperationRepository.save(operacao);

        log.info("Transação Pix concluída com sucesso. transactionId={}", event.getTransactionId());

        return AccountOperationStatus.CONCLUIDO;
    }

    private AccountOperationStatus registrarFalha(
            AccountOperation operacao,
            PixTransactionRequestedEvent event,
            AccountOperationStatus status,
            String motivo
    ) {
        operacao.setStatus(status);
        operacao.setProcessedAt(LocalDateTime.now());
        accountOperationRepository.save(operacao);

        log.warn("{}. transactionId={} status={}", motivo, event.getTransactionId(), status);

        return status;
    }

    private void publicarResultado(PixTransactionRequestedEvent event, AccountOperationStatus status) {

        accountMetrics.registrarResultado(status);

        String message = status == AccountOperationStatus.CONCLUIDO ? "Transação processada com sucesso" : "Transação não processada: " + status;

        eventPublisher.publishPixTransactionProcessed(
                PixTransactionProcessedEvent.builder()
                        .transactionId(event.getTransactionId())
                        .idempotencyKey(event.getIdempotencyKey())
                        .status(status)
                        .message(message)
                        .build()
        );
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
                .contaId(operacao.getAccountId())
                .valor(operacao.getAmount())
                .saldoApos(operacao.getSaldoApos())
                .idempotencyKey(operacao.getIdempotencyKey())
                .status(operacao.getStatus())
                .processadoEm(operacao.getProcessedAt())
                .build();
    }
}