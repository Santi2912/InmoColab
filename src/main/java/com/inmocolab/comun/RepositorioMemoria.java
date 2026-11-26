package com.inmocolab.comun;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Persistencia en memoria. Aísla al resto del código del almacenamiento: se puede
 * reemplazar por una implementación JDBC sin tocar las demás capas.
 *
 * Patrón: Repository / DAO (acceso a datos aislado del resto de la aplicación).
 */
public abstract class RepositorioMemoria<T extends Entidad> {

    private final Map<Long, T> datos = new LinkedHashMap<>();
    private long ultimo = 0;

    public synchronized T guardar(T entidad) {
        if (entidad.getId() == 0) {
            entidad.setId(++ultimo);
        }
        datos.put(entidad.getId(), entidad);
        return entidad;
    }

    /** Reinserta una entidad leída de la base de datos conservando su identificador. */
    public synchronized void restaurar(T entidad) {
        datos.put(entidad.getId(), entidad);
        ultimo = Math.max(ultimo, entidad.getId());
    }

    public synchronized T buscar(long id) {
        return datos.get(id);
    }

    public synchronized List<T> todos() {
        return new ArrayList<>(datos.values());
    }

    public List<T> filtrar(Predicate<T> condicion) {
        List<T> resultado = new ArrayList<>();
        for (T e : todos()) {
            if (condicion.test(e)) {
                resultado.add(e);
            }
        }
        return resultado;
    }

    public synchronized void eliminar(long id) {
        datos.remove(id);
    }
}
