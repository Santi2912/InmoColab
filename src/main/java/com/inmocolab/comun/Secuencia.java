package com.inmocolab.comun;

import java.util.concurrent.atomic.AtomicLong;

/** Generador de identificadores para entidades anidadas (fotos, participaciones, etc.). */
public final class Secuencia {

    private static final AtomicLong ACTUAL = new AtomicLong(0);

    private Secuencia() {
    }

    /** Garantiza que los próximos identificadores sean mayores que los ya existentes en la base de datos. */
    public static void asegurarMinimo(long valor) {
        long actual;
        while ((actual = ACTUAL.get()) < valor && !ACTUAL.compareAndSet(actual, valor)) {
            // reintenta si otro hilo cambió el valor
        }
    }

    public static long siguiente() {
        return ACTUAL.incrementAndGet();
    }
}
