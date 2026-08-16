package com.tcc.accountservice.api;

import com.tcc.accountservice.dto.AccountRequestDTO;
import com.tcc.accountservice.dto.AccountResponseDTO;
import com.tcc.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponseDTO> criar(@Valid @RequestBody AccountRequestDTO request) {
        AccountResponseDTO response = accountService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(accountService.buscarPorId(id));
    }
}
