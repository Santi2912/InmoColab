package com.inmocolab.negociacion.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.negociacion.modelo.EstadoNegociacion;
import com.inmocolab.negociacion.modelo.Negociacion;
import java.util.List;

public class NegociacionRepositorio extends RepositorioMemoria<Negociacion> {

    /** Negociación vigente (no cerrada ni cancelada) de un inmueble. */
    public Negociacion activaPorInmueble(long idInmueble) {
        for (Negociacion n : todos()) {
            if (n.getIdInmueble() == idInmueble && n.getEstado() != EstadoNegociacion.CERRADA
                    && n.getEstado() != EstadoNegociacion.CANCELADA) {
                return n;
            }
        }
        return null;
    }

    public List<Negociacion> porInmueble(long idInmueble) {
        return filtrar(n -> n.getIdInmueble() == idInmueble);
    }
}
