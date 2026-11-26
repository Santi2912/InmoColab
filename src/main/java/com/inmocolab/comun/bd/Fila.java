package com.inmocolab.comun.bd;

/** Una fila de una tabla tal como está hoy en memoria: su llave y los valores de todas las columnas. */
final class Fila {

    final String clave;
    final Object[] valores;

    Fila(Object[] valores, int nClaves) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nClaves; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(valores[i]);
        }
        this.clave = sb.toString();
        this.valores = valores;
    }
}
