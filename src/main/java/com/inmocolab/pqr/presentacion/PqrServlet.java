package com.inmocolab.pqr.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.comun.Util;
import com.inmocolab.pqr.modelo.Adjunto;
import com.inmocolab.pqr.modelo.EstadoPqr;
import com.inmocolab.pqr.modelo.PQR;
import com.inmocolab.pqr.modelo.TipoPqr;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/** Peticiones, quejas y reclamos (CU-05). */
@WebServlet(name = "PqrServlet", urlPatterns = "/ms/pqr/*")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class PqrServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Usuario u = usuario(req);
        switch (accion(req)) {
            case "lista":
                req.setAttribute("pqrs", c.pqr.listar(u));
                vista(req, resp, "pqrLista", u.getRol() == Rol.ADMINISTRADOR ? "Todas las PQR" : "Mis PQR");
                break;
            case "radicar":
                if (u.getRol() == Rol.ADMINISTRADOR) {
                    throw new NegocioException("El administrador atiende las PQR; no las radica.");
                }
                req.setAttribute("inmuebles", c.inmuebles.todos());
                vista(req, resp, "pqrRadicar", "Radicar PQR");
                break;
            case "ver": {
                PQR p = c.pqr.ver(u, lng(req, "id"));
                req.setAttribute("pqr", p);
                req.setAttribute("esAdmin", u.getRol() == Rol.ADMINISTRADOR);
                req.setAttribute("esDueno", p.getIdUsuario() == u.getId());
                req.setAttribute("puedeResponder", u.getRol() == Rol.ADMINISTRADOR
                        || (u.getRol() == Rol.ASESOR && p.getIdResponsable() != null && p.getIdResponsable() == u.getId()));
                List<Usuario> responsables = new ArrayList<>(c.usuarios.porRol(Rol.ASESOR));
                responsables.addAll(c.usuarios.porRol(Rol.ADMINISTRADOR));
                req.setAttribute("responsables", responsables);
                vista(req, resp, "pqrDetalle", "PQR " + p.getRadicado());
                break;
            }
            case "vencidas": {
                exigir(req, Rol.ADMINISTRADOR);
                EstadoPqr estado = enumOpc(EstadoPqr.class, txt(req, "estado"));
                TipoPqr tipo = enumOpc(TipoPqr.class, txt(req, "tipo"));
                req.setAttribute("pqrs", c.pqr.vencidas(estado, tipo, fecha(req, "desde"), fecha(req, "hasta")));
                vista(req, resp, "pqrVencidas", "PQR vencidas o por vencer");
                break;
            }
            default:
                throw noEncontrado();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Usuario u = usuario(req);
        switch (accion(req)) {
            case "radicar": {
                List<Adjunto> adjuntos = adjuntosDe(req);
                TipoPqr tipo = enumOpc(TipoPqr.class, txt(req, "tipo"));
                PQR p = c.pqr.radicar(u, tipo, txt(req, "asunto"), txt(req, "descripcion"), lngOpc(req, "idInmueble"),
                        lngOpc(req, "idNegociacion"), adjuntos);
                exito(req, resp, "PQR radicada con el número " + p.getRadicado() + ". Fecha límite de respuesta: " + Util.fecha(p.getFechaLimite()) + ".",
                        "/api/pqr/ver?id=" + p.getId());
                break;
            }
            case "asignar": {
                long id = lng(req, "id");
                c.pqr.asignar(u, id, lng(req, "idResponsable"));
                exito(req, resp, "PQR asignada; el responsable fue notificado.", "/api/pqr/ver?id=" + id);
                break;
            }
            case "responder": {
                long id = lng(req, "id");
                c.pqr.responder(u, id, txt(req, "texto"));
                exito(req, resp, "Respuesta registrada y usuario notificado.", "/api/pqr/ver?id=" + id);
                break;
            }
            case "cerrar": {
                long id = lng(req, "id");
                Integer calificacion = null;
                if (!txt(req, "calificacion").isEmpty()) {
                    try {
                        calificacion = Integer.valueOf(txt(req, "calificacion"));
                    } catch (NumberFormatException e) {
                        throw new NegocioException("La calificación va de 1 a 5.");
                    }
                }
                c.pqr.cerrar(u, id, calificacion);
                exito(req, resp, "PQR cerrada. ¡Gracias por tu calificación!", "/api/pqr/ver?id=" + id);
                break;
            }
            case "reabrir": {
                long id = lng(req, "id");
                c.pqr.reabrir(u, id);
                exito(req, resp, "PQR reabierta.", "/api/pqr/ver?id=" + id);
                break;
            }
            default:
                throw noEncontrado();
        }
    }

    private List<Adjunto> adjuntosDe(HttpServletRequest req) throws IOException, ServletException {
        List<Adjunto> adjuntos = new ArrayList<>();
        try {
            for (Part p : req.getParts()) {
                if ("adjuntos".equals(p.getName()) && p.getSize() > 0 && !Util.vacio(p.getSubmittedFileName())) {
                    adjuntos.add(new Adjunto(p.getSubmittedFileName(), p.getSize()));
                }
            }
        } catch (IllegalStateException e) {
            throw new NegocioException("Un adjunto supera 5 MB o el envío es demasiado grande.");
        }
        return adjuntos;
    }

    private <E extends Enum<E>> E enumOpc(Class<E> tipo, String valor) {
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Valor no válido: " + valor);
        }
    }
}
