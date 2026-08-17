package com.tcc.accountservice.exception;

public class SaldoInsuficienteException extends RuntimeException {
    public SaldoInsuficienteException(String contaId) {
        super("Saldo insuficiente para débito na conta: " + contaId);
    }
}