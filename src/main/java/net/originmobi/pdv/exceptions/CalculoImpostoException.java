package net.originmobi.pdv.exceptions;

public class CalculoImpostoException extends RuntimeException {

    public CalculoImpostoException(String message) {
        super(message);
    }

    public CalculoImpostoException(String message, Throwable cause) {
        super(message, cause);
    }
}