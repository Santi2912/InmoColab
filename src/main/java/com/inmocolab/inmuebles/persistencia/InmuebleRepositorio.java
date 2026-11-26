package com.inmocolab.inmuebles.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.inmuebles.modelo.Inmueble;
import java.util.List;

public class InmuebleRepositorio extends RepositorioMemoria<Inmueble> {

    public List<Inmueble> porPropietario(long idPropietario) {
        return filtrar(i -> i.getIdPropietario() == idPropietario);
    }
}
