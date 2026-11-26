package com.inmocolab.pqr.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.pqr.modelo.PQR;
import java.util.List;

public class PqrRepositorio extends RepositorioMemoria<PQR> {

    public List<PQR> porUsuario(long idUsuario) {
        return filtrar(p -> p.getIdUsuario() == idUsuario);
    }
}
