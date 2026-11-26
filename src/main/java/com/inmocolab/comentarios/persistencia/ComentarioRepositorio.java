package com.inmocolab.comentarios.persistencia;

import com.inmocolab.comentarios.modelo.Comentario;
import com.inmocolab.comun.RepositorioMemoria;
import java.util.List;

public class ComentarioRepositorio extends RepositorioMemoria<Comentario> {

    public List<Comentario> porInmueble(long idInmueble) {
        return filtrar(c -> c.getIdInmueble() == idInmueble);
    }
}
