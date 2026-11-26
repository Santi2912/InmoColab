package com.inmocolab.comun;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Mensajes de una sola vez (éxito o error) que sobreviven a una redirección. */
public final class Flash {

    private static final String MENSAJE = "flash.mensaje";
    private static final String TIPO = "flash.tipo";

    private Flash() {
    }

    public static void exito(HttpServletRequest req, String mensaje) {
        poner(req, "ok", mensaje);
    }

    public static void error(HttpServletRequest req, String mensaje) {
        poner(req, "error", mensaje);
    }

    private static void poner(HttpServletRequest req, String tipo, String mensaje) {
        HttpSession s = req.getSession(true);
        s.setAttribute(TIPO, tipo);
        s.setAttribute(MENSAJE, mensaje);
    }

    /** Devuelve {tipo, mensaje} y lo borra, o null si no hay. */
    public static String[] tomar(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute(MENSAJE) == null) {
            return null;
        }
        String[] r = {(String) s.getAttribute(TIPO), (String) s.getAttribute(MENSAJE)};
        s.removeAttribute(TIPO);
        s.removeAttribute(MENSAJE);
        return r;
    }

    /** Conserva lo digitado en un formulario que falló (sin contraseñas) para repoblarlo. */
    public static void guardarFormulario(HttpServletRequest req) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            if (!e.getKey().toLowerCase().contains("clave") && e.getValue().length > 0) {
                valores.put(e.getKey(), e.getValue()[0]);
            }
        }
        req.getSession(true).setAttribute("form", valores);
    }

    public static void limpiarFormulario(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.removeAttribute("form");
        }
    }
}
