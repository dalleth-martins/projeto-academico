package com.tcc.accountservice.repository;

import com.tcc.accountservice.entidade.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends MongoRepository<Account, String> {

    List<Account> findByClienteId(String clienteId);

    boolean existsByNumeroConta(String numeroConta);
}