package com.inmocolab.compracolaborativa.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.compracolaborativa.modelo.EstadoGrupo;
import com.inmocolab.compracolaborativa.modelo.EstadoParticipacion;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.compracolaborativa.modelo.Participacion;
import java.time.LocalDate;
import java.util.List;

public class GrupoRepositorio extends RepositorioMemoria<GrupoCompra> {

    public List<GrupoCompra> porInmueble(long idInmueble) {
        return filtrar(g -> g.getIdInmueble() == idInmueble);
    }

    /** Grupos que aún aceptan integrantes. */
    public List<GrupoCompra> abiertosPorInmueble(long idInmueble) {
        LocalDate hoy = LocalDate.now();
        return filtrar(g -> g.getIdInmueble() == idInmueble && g.getEstado() == EstadoGrupo.ABIERTO
                && !g.getFechaLimite().isBefore(hoy));
    }

    public List<GrupoCompra> porIntegrante(long idComprador) {
        return filtrar(g -> {
            for (Participacion p : g.getParticipaciones()) {
                if (p.getIdComprador() == idComprador && p.getEstado() == EstadoParticipacion.ACTIVA) {
                    return true;
                }
            }
            return false;
        });
    }
}
