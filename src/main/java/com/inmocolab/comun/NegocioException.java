package com.inmocolab.comun;

/** Error de regla de negocio; su mensaje se muestra al usuario. */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
