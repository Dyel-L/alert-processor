package com.ubisafe.alert_processor.exception;

/**
 * Exceção específica para erros de desserialização de JSON inválido.
 * Permite tratamento específico sem depender de string matching.
 */
public class InvalidAlertJsonException extends AlertProcessingException {

    public InvalidAlertJsonException(String message) {
        super(message);
    }

    public InvalidAlertJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}

