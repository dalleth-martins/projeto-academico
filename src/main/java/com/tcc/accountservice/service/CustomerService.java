package com.tcc.accountservice.service;

import com.tcc.accountservice.dto.CustomerRequestDTO;
import com.tcc.accountservice.dto.CustomerResponseDTO;
import com.tcc.accountservice.entidade.Customer;
import com.tcc.accountservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerResponseDTO cadastrar(CustomerRequestDTO request) {
        if (customerRepository.existsByCpf(request.cpf())) {
            throw new CpfAlreadyExistsException(request.cpf());
        }

        Customer customer = Customer.builder()
                .cpf(request.cpf())
                .nome(request.nome())
                .email(request.email())
                .telefone(request.telefone())
                .dataNascimento(request.dataNascimento())
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