package com.inmocolab.comun;

import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Usuario;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Acceso a la sesión del usuario autenticado. */
public final class Sesion {

    private static final String ATRIBUTO = "uid";

    private Sesion() {
    }

    /** Usuario autenticado y activo, o null. */
    public static Usuario actual(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) {
            return null;
        }
        Object uid = s.getAttribute(ATRIBUTO);
        if (!(uid instanceof Long)) {
            return null;
        }
        Usuario u = Contexto.get().usuarios.buscar((Long) uid);
        if (u == null || u.getEstado() != EstadoUsuario.ACTIVO) {
            s.removeAttribute(ATRIBUTO);
            return null;
        }
        return u;
    }

    public static void iniciar(HttpServletRequest req, Usuario u) {
        req.getSession(true);
        req.changeSessionId();
        req.getSession().setAttribute(ATRIBUTO, u.getId());
    }

    public static void cerrar(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.invalidate();
        }
    }
}
