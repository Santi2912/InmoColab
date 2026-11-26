package com.inmocolab.negociacion.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.negociacion.modelo.Interes;
import java.util.List;

public class InteresRepositorio extends RepositorioMemoria<Interes> {

    public List<Interes> porInmueble(long idInmueble) {
        return filtrar(i -> i.getIdInmueble() == idInmueble);
    }

    public boolean existe(long idInmueble, long idComprador) {
        return !filtrar(i -> i.getIdInmueble() == idInmueble && i.getIdComprador() == idComprador).isEmpty();
    }
}
