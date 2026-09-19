package com.tcc.accountservice.api;

import com.tcc.accountservice.dto.request.CustomerRequestDTO;
import com.tcc.accountservice.dto.response.CustomerResponseDTO;
import com.tcc.accountservice.service.AccountService;
import com.tcc.accountservice.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponseDTO> cadastrarCliente(@Valid @RequestBody CustomerRequestDTO request) {

        log.info("Iniciando cadastro de cliente");

        CustomerResponseDTO response = customerService.cadastrarCliente(request);

        log.info("Cliente cadastrado com sucesso. id={}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<CustomerResponseDTO> buscarPorId(@PathVariable String id) {
//        return ResponseEntity.ok(customerService.buscarPorId(id));
//    }


    @GetMapping("/{documento}")
    public ResponseEntity<CustomerResponseDTO> buscarPeloDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(customerService.buscarPeloDocumento(documento));
    }
}
