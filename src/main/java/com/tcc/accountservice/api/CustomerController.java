package com.tcc.accountservice.api;

import com.tcc.accountservice.dto.CustomerRequestDTO;
import com.tcc.accountservice.dto.CustomerResponseDTO;
import com.tcc.accountservice.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponseDTO> cadastrar(@Valid @RequestBody CustomerRequestDTO request) {
        CustomerResponseDTO response = customerService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(customerService.buscarPorId(id));
    }
}
