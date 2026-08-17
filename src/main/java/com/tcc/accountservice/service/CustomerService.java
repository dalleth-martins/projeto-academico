package com.tcc.accountservice.service;

import com.tcc.accountservice.dto.request.CustomerRequestDTO;
import com.tcc.accountservice.dto.response.CustomerResponseDTO;
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

    public CustomerResponseDTO cadastrarCliente(CustomerRequestDTO request) {
        if (customerRepository.existsByDocumento(request.getDocumento())) {
            throw new CpfAlreadyExistsException(request.getDocumento());
        }
        Customer customer = Customer.builder()
                .documento(request.getDocumento())
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

    public CustomerResponseDTO buscarPeloDocumento(String documento) {
        return customerRepository.findByDocumento(documento)
                .map(customer -> {
                    CustomerResponseDTO dto = new CustomerResponseDTO();

                    dto.setId(customer.getId());
                    dto.setNome(customer.getNome());
                    dto.setDocumento(customer.getDocumento());

                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }

    private CustomerResponseDTO toResponseDTO(Customer customer) {
        return new CustomerResponseDTO(
                customer.getId(),
                customer.getDocumento(),
                customer.getNome(),
                customer.getEmail(),
                customer.getTelefone(),
                customer.getDataNascimento(),
                customer.getCriadoEm()
        );
    }
}