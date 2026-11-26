package com.inmocolab.usuarios.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.usuarios.modelo.Notificacion;
import java.util.Collections;
import java.util.List;

public class NotificacionRepositorio extends RepositorioMemoria<Notificacion> {

    /** Más recientes primero. */
    public List<Notificacion> porUsuario(long idUsuario) {
        List<Notificacion> lista = filtrar(n -> n.getIdUsuario() == idUsuario);
        Collections.reverse(lista);
        return lista;
    }

    public int noLeidas(long idUsuario) {
        return filtrar(n -> n.getIdUsuario() == idUsuario && !n.isLeida()).size();
    }
}
