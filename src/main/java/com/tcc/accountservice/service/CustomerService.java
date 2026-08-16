package com.tcc.accountservice.service;

import com.tcc.accountservice.dto.CustomerRequestDTO;
import com.tcc.accountservice.dto.CustomerResponseDTO;
import com.tcc.accountservice.entidade.Customer;
import com.tcc.accountservice.exception.CpfAlreadyExistsException;
import com.tcc.accountservice.exception.CustomerNotFoundException;
import com.tcc.accountservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerResponseDTO cadastrar(CustomerRequestDTO request) {
        if (customerRepository.existsByCpf(request.getCpf())) {
            throw new CpfAlreadyExistsException(request.getCpf());
        }

        Customer customer = Customer.builder()
                .cpf(request.getCpf())
                .nome(request.getNome())
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .dataNascimento(request.getDataNascimento())
                .build();

        Customer salvo = customerRepository.save(customer);
        return toResponseDTO(salvo);
    }

    public CustomerResponseDTO buscarPorId(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return toResponseDTO(customer);
    }

    public boolean existePorId(String id) {
        return customerRepository.existsById(id);
    }

    private CustomerResponseDTO toResponseDTO(Customer customer) {
        return new CustomerResponseDTO(
                customer.getId(),
                customer.getCpf(),
                customer.getNome(),
                customer.getEmail(),
                customer.getTelefone(),
                customer.getDataNascimento(),
                customer.getCriadoEm()
        );
    }
}