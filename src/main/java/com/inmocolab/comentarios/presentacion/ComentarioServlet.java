package com.inmocolab.comentarios.presentacion;

import com.inmocolab.comentarios.modelo.Comentario;
import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Comentarios sobre inmuebles y su moderación (CU-06). */
@WebServlet(name = "ComentarioServlet", urlPatterns = "/ms/comentarios/*")
public class ComentarioServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        exigir(req, Rol.ADMINISTRADOR);
        if (!"moderar".equals(accion(req))) {
            throw noEncontrado();
        }
        req.setAttribute("comentarios", Contexto.get().moderacion.reportados());
        vista(req, resp, "moderar", "Moderar comentarios");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Usuario u = usuario(req);
        switch (accion(req)) {
            case "publicar": {
                long idInmueble = lng(req, "idInmueble");
                c.comentarios.publicar(u, idInmueble, txt(req, "texto"));
                exito(req, resp, "Comentario publicado.", detalle(idInmueble));
                break;
            }
            case "responder": {
                Comentario cm = c.comentarios.requerir(lng(req, "id"));
                c.comentarios.responder(u, cm.getId(), txt(req, "texto"));
                exito(req, resp, "Respuesta publicada; el autor fue notificado.", detalle(cm.getIdInmueble()));
                break;
            }
            case "editar": {
                Comentario cm = c.comentarios.requerir(lng(req, "id"));
                c.comentarios.editar(u, cm.getId(), txt(req, "texto"));
                exito(req, resp, "Comentario actualizado.", detalle(cm.getIdInmueble()));
                break;
            }
            case "eliminar": {
                long idInmueble = c.comentarios.eliminar(u, lng(req, "id"));
                exito(req, resp, "Comentario eliminado.", detalle(idInmueble));
                break;
            }
            case "reportar": {
                Comentario cm = c.comentarios.requerir(lng(req, "id"));
                c.comentarios.reportar(u, cm.getId(), txt(req, "motivo"));
                exito(req, resp, "Gracias, el comentario fue reportado.", detalle(cm.getIdInmueble()));
                break;
            }
            case "moderar": {
                Usuario admin = exigir(req, Rol.ADMINISTRADOR);
                long id = lng(req, "id");
                String decision = txt(req, "decision");
                if ("aprobar".equals(decision)) {
                    c.moderacion.aprobar(admin, id);
                } else if ("ocultar".equals(decision)) {
                    c.moderacion.ocultar(admin, id, txt(req, "motivo"));
                } else {
                    throw new NegocioException("Selecciona aprobar u ocultar.");
                }
                exito(req, resp, "Decisión registrada y autor notificado.", "/api/comentarios/moderar");
                break;
            }
            default:
                throw noEncontrado();
        }
    }

    private String detalle(long idInmueble) {
        return "/api/inmuebles/detalle?id=" + idInmueble + "#comentarios";
    }
}
