package com.inmocolab.usuarios.persistencia;

import com.inmocolab.usuarios.modelo.Bitacora;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Solo permite agregar y consultar: la bitácora no se modifica ni se elimina. */
public class BitacoraRepositorio {

    private final List<Bitacora> registros = new ArrayList<>();

    public synchronized void registrar(long idUsuario, String accion, String detalle) {
        registros.add(new Bitacora(registros.size() + 1, idUsuario, accion, detalle, LocalDateTime.now()));
    }

    /** Reinserta un registro leído de la base de datos (se llama en orden ascendente de id). */
    public synchronized void restaurar(Bitacora registro) {
        registros.add(registro);
    }

    /** Más recientes primero. */
    public synchronized List<Bitacora> todas() {
        List<Bitacora> copia = new ArrayList<>(registros);
        Collections.reverse(copia);
        return copia;
    }
}
