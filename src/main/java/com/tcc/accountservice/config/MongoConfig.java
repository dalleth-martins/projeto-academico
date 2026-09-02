package com.tcc.accountservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    @Bean
    public MongoTransactionManager transactionManager(
            MongoDatabaseFactory mongoDatabaseFactory) {

        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}
