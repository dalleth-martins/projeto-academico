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
public class AccountCreditRepository {

    private final MongoTemplate mongoTemplate;

    public Optional<Account> creditar(String accountId, BigDecimal amount) {

        Query query = new Query(
                Criteria.where("_id").is(accountId)
                        .and("status").is(AccountStatus.ATIVA)
        );

        Update update = new Update()
                .inc("saldo", amount);

        Account account = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Account.class
        );

        return Optional.ofNullable(account);
    }
}