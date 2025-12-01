package net.originmobi.pdv.exceptions;

public class CartaoLancamentoException extends RuntimeException {

    public CartaoLancamentoException(String message) {
        super(message);
    }

    public CartaoLancamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}