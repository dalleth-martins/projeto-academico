package com.tcc.accountservice.repository;

import com.tcc.accountservice.entidade.AccountOperation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AccountOperationRepository
        extends MongoRepository<AccountOperation, String> {

    Optional<AccountOperation> findByIdempotencyKey(String idempotencyKey);
}
