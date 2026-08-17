package com.tcc.accountservice.repository;

import com.tcc.accountservice.entidade.DebitOperation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DebitOperationRepository extends MongoRepository<DebitOperation, String> {
    Optional<DebitOperation> findByIdempotencyKey(String idempotencyKey);
}
