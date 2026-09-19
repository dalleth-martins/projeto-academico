package com.tcc.accountservice.api;

import com.tcc.accountservice.dto.request.AccountRequestDTO;
import com.tcc.accountservice.dto.request.BalanceResponseDTO;
import com.tcc.accountservice.dto.request.CreditRequestDTO;
import com.tcc.accountservice.dto.request.DebitRequestDTO;
import com.tcc.accountservice.dto.response.AccountResponseDTO;
import com.tcc.accountservice.dto.response.DebitResponseDTO;
import com.tcc.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;


    @PostMapping
    public ResponseEntity<AccountResponseDTO> criarConta(@Valid @RequestBody AccountRequestDTO request) {

        log.info("Iniciando criação de conta para o cliente");

        AccountResponseDTO response = accountService.criar(request);

        log.info("Conta criada com sucesso. numeroConta={}", response.getNumeroConta());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(accountService.buscarPorId(id));
    }

    @PostMapping("/{accountId}/debit")
    public ResponseEntity<DebitResponseDTO> debitar(@PathVariable String accountId,
                                                    @Valid @RequestBody DebitRequestDTO request) {
        log.info("Solicitação de débito. contaId={} valor={}", accountId, request.getValor());
        DebitResponseDTO response = accountService.debitar(accountId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/credit")
    public ResponseEntity<AccountResponseDTO> creditar(@PathVariable String accountId,
                                                       @RequestBody CreditRequestDTO request) {

        AccountResponseDTO response = accountService.creditar(accountId, request.getValor());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponseDTO> consultarSaldo(
            @PathVariable String accountId) {

        log.info("Solicitação de consulta de saldo. accountId={}", accountId);

        BalanceResponseDTO response = accountService.consultarSaldo(accountId);

        log.info("Saldo consultado com sucesso. accountId={} saldo={}", accountId, response.getSaldo());

        return ResponseEntity.ok(response);
    }
}
