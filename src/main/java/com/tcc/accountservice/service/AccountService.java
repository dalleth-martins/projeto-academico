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

    /**
     * Auto-injeção do proxy do Spring (não pode ser "final"/via construtor,
     * senão vira dependência circular na inicialização do bean; @Lazy resolve
     * isso adiando a busca do bean até o primeiro uso real).
     * <p>
     * Motivo: se processarTransacaoAtomicamente fosse chamado como
     * "this.processarTransacaoAtomicamente(...)" dentro desta mesma classe,
     * a chamada não passaria pelo proxy do Spring e o @Transactional seria
     * silenciosamente ignorado (auto-invocação é uma limitação conhecida do
     * AOP baseado em proxy). Chamando via "self.", garantimos que a chamada
     * passe pelo proxy e a transação seja realmente aberta.
     */
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

    public DebitResponseDTO debitar(String contaId, DebitRequestDTO request) {

        AccountOperation operacao = AccountOperation.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .accountId(contaId)
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

        Account debitado = accountDebitRepository.debitarSeSaldoSuficiente(contaId, request.getValor()).orElse(null);

        if (debitado == null) {

            boolean contaExiste = accountRepository.existsById(contaId);

            operacao.setStatus(contaExiste ? AccountOperationStatus.FALHOU_SALDO_INSUFICIENTE : AccountOperationStatus.FALHOU_CONTA_INVALIDA);

            operacao.setProcessedAt(LocalDateTime.now());

            accountOperationRepository.save(operacao);

            if (!contaExiste) {
                throw new AccountNotFoundException(contaId);
            }

            throw new SaldoInsuficienteException(contaId);
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

    /**
     * Orquestra o processamento de uma transação Pix recebida via evento.
     * <p>
     * Erros de negócio conhecidos (conta inválida, saldo insuficiente,
     * corrida de idempotência) são tratados aqui e sempre resultam na
     * publicação de um PixTransactionProcessedEvent — a mensagem é
     * confirmada (ack) no RabbitMQ normalmente nesses casos.
     * <p>
     * Erros inesperados (falha de infraestrutura, bug) NÃO são capturados
     * aqui de propósito: eles propagam para o PixTransactionListener, que
     * decide entre retry e envio para a dead-letter queue.
     */
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

            // Chamada via "self", não "this" — ver comentário no campo self
            // logo acima. É o que garante que o @Transactional do método
            // abaixo realmente abra uma transação MongoDB.
            statusFinal = self.processarTransacaoAtomicamente(event);

        } catch (DuplicateKeyException e) {

            // Outra thread/instância inseriu a mesma idempotencyKey entre
            // o check acima e o insert dentro do método atômico.
            log.info("Operação já registrada por outra requisição concorrente. transactionId={} idempotencyKey={}",
                    event.getTransactionId(), event.getIdempotencyKey());

            statusFinal = accountOperationRepository.findByIdempotencyKey(event.getIdempotencyKey())
                    .map(AccountOperation::getStatus)
                    .orElse(AccountOperationStatus.FALHOU_CONTA_INVALIDA);

        } catch (AccountNotFoundException e) {

            // Conta destino ficou inválida depois do débito na origem
            // (rollback já ocorreu dentro do método atômico via @Transactional).
            log.warn("Transação revertida: conta inválida. transactionId={} motivo={}",
                    event.getTransactionId(), e.getMessage());

            statusFinal = AccountOperationStatus.FALHOU_CONTA_INVALIDA;
        }

        publicarResultado(event, statusFinal);
    }

    /**
     * Executa o débito na origem + crédito no destino dentro de uma única
     * transação MongoDB multi-documento (replica set configurado em
     * infra/docker-compose.yml + MongoTransactionManager em MongoConfig).
     * <p>
     * Falhas de negócio conhecidas (saldo insuficiente, conta inválida)
     * fazem o método retornar normalmente — a transação é COMMITADA com o
     * status de falha já registrado, já que nenhum dinheiro chegou a ser
     * movido nesses casos. Só lançamos exceção no cenário raro em que a
     * conta destino se torna inválida DEPOIS do débito na origem: aí sim
     * precisamos que a transação inteira reverta.
     * <p>
     * ATENÇÃO: nunca chame este método via "this.processarTransacaoAtomicamente(...)"
     * dentro da própria classe — chame sempre via "self." (ver campo self
     * acima), senão o @Transactional é ignorado silenciosamente.
     */
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

        // Se isto lançar DuplicateKeyException, nada mais foi persistido
        // ou movimentado ainda — quem chamou (processarTransacao) trata a
        // exceção consultando o registro já existente.
        accountOperationRepository.insert(operacao);

        // Valida a conta destino ANTES de mexer em qualquer saldo. Evita o
        // caso comum (destinationAccountId digitado errado) ter que passar
        // pelo caminho de rollback via exceção lá embaixo.
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

            // Cenário raro: a conta destino existia no check acima mas
            // ficou inválida (ex: desativada) entre o check e o crédito.
            // O débito na origem já foi aplicado, então precisamos que a
            // transação inteira reverta — por isso lançamos a exceção em
            // vez de retornar um status. NÃO salvamos "operacao" aqui:
            // qualquer save seria desfeito no rollback de qualquer forma.
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