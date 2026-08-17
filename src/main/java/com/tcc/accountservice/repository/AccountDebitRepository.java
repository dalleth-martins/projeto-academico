package com.tcc.accountservice.repository;

import com.tcc.accountservice.entidade.Account;
import com.tcc.accountservice.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountDebitRepository {
    private final MongoTemplate mongoTemplate;

    public Optional<Account> debitarSeSaldoSuficiente(String contaId, BigDecimal valor) {
        Query query = new Query(Criteria.where("id").is(contaId)
                .and("status").is(AccountStatus.ATIVA)
                .and("saldo").gte(valor));

        Update update = new Update().inc("saldo", valor.negate());

        Account atualizado = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Account.class
        );

        return Optional.ofNullable(atualizado);
    }
}
