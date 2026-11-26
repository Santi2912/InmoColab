package com.inmocolab.comentarios.persistencia;

import com.inmocolab.comentarios.modelo.Calificacion;
import com.inmocolab.comun.RepositorioMemoria;

public class CalificacionRepositorio extends RepositorioMemoria<Calificacion> {

    public boolean existe(long idNegociacion, long idComprador) {
        return !filtrar(c -> c.getIdNegociacion() == idNegociacion && c.getIdComprador() == idComprador).isEmpty();
    }
}
