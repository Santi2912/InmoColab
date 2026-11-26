package com.inmocolab.negociacion.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.negociacion.modelo.Negociacion;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Interés, seguimiento, cierre y acompañamiento de negociaciones (CU-04). */
@WebServlet(name = "NegociacionServlet", urlPatterns = "/ms/negociaciones/*")
public class NegociacionServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Usuario u = usuario(req);
        switch (accion(req)) {
            case "mis":
                req.setAttribute("negociaciones", c.negociaciones.misNegociaciones(u));
                vista(req, resp, "negociaciones", u.getRol() == Rol.ASESOR ? "Negociaciones asignadas" : "Negociaciones");
                break;
            case "ver": {
                Negociacion n = c.negociaciones.ver(u, lng(req, "id"));
                Inmueble i = c.inmuebles.requerir(n.getIdInmueble());
                req.setAttribute("negociacion", n);
                req.setAttribute("inmueble", i);
                req.setAttribute("esPropietario", u.getRol() == Rol.PROPIETARIO && i.getIdPropietario() == u.getId());
                req.setAttribute("esAsesor", u.getRol() == Rol.ASESOR && n.getIdAsesor() != null && n.getIdAsesor() == u.getId());
                req.setAttribute("puedeCalificar", c.comentarios.puedeCalificar(u, n));
                req.setAttribute("calificaciones", c.comentarios.calificacionesDe(n.getId()));
                if (u.getRol() == Rol.ADMINISTRADOR) {
                    req.setAttribute("asesores", c.usuarios.porRol(Rol.ASESOR));
                }
                vista(req, resp, "negociacion", "Negociación #" + n.getId());
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
            case "interes": {
                long idInmueble = lng(req, "idInmueble");
                c.negociaciones.manifestarInteres(u, idInmueble);
                exito(req, resp, "Interés registrado. El propietario fue notificado.", "/api/inmuebles/detalle?id=" + idInmueble);
                break;
            }
            case "observacion": {
                long id = lng(req, "id");
                c.negociaciones.observar(u, id, txt(req, "texto"));
                exito(req, resp, "Observación registrada.", "/api/negociaciones/ver?id=" + id);
                break;
            }
            case "cerrar": {
                long id = lng(req, "id");
                c.negociaciones.cerrar(u, id);
                exito(req, resp, "Negociación cerrada: el inmueble quedó como vendido.", "/api/negociaciones/ver?id=" + id);
                break;
            }
            case "cancelar": {
                long id = lng(req, "id");
                c.negociaciones.cancelar(u, id, txt(req, "motivo"));
                exito(req, resp, "Negociación cancelada.", "/api/negociaciones/ver?id=" + id);
                break;
            }
            case "asignar": {
                Usuario admin = exigir(req, Rol.ADMINISTRADOR);
                long id = lng(req, "id");
                c.negociaciones.asignarAsesor(admin, id, lng(req, "idAsesor"));
                exito(req, resp, "Asesor asignado y notificado.", "/api/negociaciones/ver?id=" + id);
                break;
            }
            case "calificar": {
                long id = lng(req, "id");
                int estrellas;
                try {
                    estrellas = Integer.parseInt(txt(req, "estrellas"));
                } catch (NumberFormatException e) {
                    throw new NegocioException("Selecciona de 1 a 5 estrellas.");
                }
                c.comentarios.calificar(u, id, estrellas, txt(req, "texto"));
                exito(req, resp, "¡Gracias por calificar tu experiencia!", "/api/negociaciones/ver?id=" + id);
                break;
            }
            default:
                throw noEncontrado();
        }
    }
}
