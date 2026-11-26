package com.inmocolab.inmuebles.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Validación de publicaciones por el administrador (CU-08.1). */
@WebServlet(name = "PublicacionServlet", urlPatterns = "/ms/publicaciones/*")
public class PublicacionServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        exigir(req, Rol.ADMINISTRADOR);
        if (!"pendientes".equals(accion(req))) {
            throw noEncontrado();
        }
        req.setAttribute("pendientes", Contexto.get().publicaciones.pendientes());
        vista(req, resp, "pendientes", "Publicaciones pendientes");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario admin = exigir(req, Rol.ADMINISTRADOR);
        Contexto c = Contexto.get();
        switch (accion(req)) {
            case "aprobar":
                c.publicaciones.aprobar(admin, lng(req, "id"));
                exito(req, resp, "Publicación aprobada y propietario notificado.", "/api/publicaciones/pendientes");
                break;
            case "rechazar":
                c.publicaciones.rechazar(admin, lng(req, "id"), txt(req, "motivo"));
                exito(req, resp, "Publicación rechazada y propietario notificado.", "/api/publicaciones/pendientes");
                break;
            default:
                throw noEncontrado();
        }
    }
}
