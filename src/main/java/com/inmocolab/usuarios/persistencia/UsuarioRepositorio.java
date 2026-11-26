package com.inmocolab.usuarios.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.util.List;

public class UsuarioRepositorio extends RepositorioMemoria<Usuario> {

    public Usuario porCorreo(String correo) {
        for (Usuario u : todos()) {
            if (u.getCorreo().equalsIgnoreCase(correo)) {
                return u;
            }
        }
        return null;
    }

    public Usuario porDocumento(String documento) {
        for (Usuario u : todos()) {
            if (u.getDocumento().equals(documento)) {
                return u;
            }
        }
        return null;
    }

    public Usuario porToken(String token) {
        for (Usuario u : todos()) {
            if (token != null && token.equals(u.getTokenVerificacion())) {
                return u;
            }
        }
        return null;
    }

    public List<Usuario> porRol(Rol rol) {
        return filtrar(u -> u.getRol() == rol);
    }
}
