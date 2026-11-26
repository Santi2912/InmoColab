package com.inmocolab.usuarios.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.comun.Util;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Consulta de notificaciones (CU-01.4). */
@WebServlet(name = "NotificacionServlet", urlPatterns = "/ms/notificaciones/*")
public class NotificacionServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario u = usuario(req);
        switch (accion(req)) {
            case "lista":
            case "":
                req.setAttribute("notificaciones", Contexto.get().usuarios.notificaciones(u.getId()));
                vista(req, resp, "notificaciones", "Notificaciones");
                break;
            case "abrir": {
                String enlace = Contexto.get().usuarios.abrirNotificacion(u.getId(), lng(req, "id"));
                ir(req, resp, Util.vacio(enlace) ? "/api/notificaciones/lista" : enlace);
                break;
            }
            default:
                throw noEncontrado();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario u = usuario(req);
        if ("leerTodas".equals(accion(req))) {
            Contexto.get().usuarios.marcarTodasLeidas(u.getId());
            ir(req, resp, "/api/notificaciones/lista");
        } else {
            throw noEncontrado();
        }
    }
}
