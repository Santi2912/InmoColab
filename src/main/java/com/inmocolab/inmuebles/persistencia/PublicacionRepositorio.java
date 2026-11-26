package com.inmocolab.inmuebles.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.inmuebles.modelo.EstadoPublicacion;
import com.inmocolab.inmuebles.modelo.Publicacion;
import java.util.List;

public class PublicacionRepositorio extends RepositorioMemoria<Publicacion> {

    public Publicacion porInmueble(long idInmueble) {
        for (Publicacion p : todos()) {
            if (p.getIdInmueble() == idInmueble) {
                return p;
            }
        }
        return null;
    }

    public List<Publicacion> porEstado(EstadoPublicacion estado) {
        return filtrar(p -> p.getEstado() == estado);
    }
}
