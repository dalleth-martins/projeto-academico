package com.tcc.accountservice.repository;

import com.tcc.accountservice.entidade.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, String> {

    boolean existsByDocumento(String cpf);

    Optional<Customer> findByDocumento(String cpf);
}