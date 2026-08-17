package com.tcc.accountservice.api;

import com.tcc.accountservice.dto.request.AccountRequestDTO;
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

    @PostMapping("/{id}/debit")
    public ResponseEntity<DebitResponseDTO> debitar(@PathVariable String id,
                                                    @Valid @RequestBody DebitRequestDTO request) {
        log.info("Solicitação de débito. contaId={} valor={}", id, request.getValor());
        DebitResponseDTO response = accountService.debitar(id, request);
        return ResponseEntity.ok(response);
    }
}
