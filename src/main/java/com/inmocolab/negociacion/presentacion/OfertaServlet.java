package com.inmocolab.negociacion.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.negociacion.modelo.Oferta;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Ofertas: realizar, retirar, aceptar y rechazar (CU-04.2, 04.4 y 04.5). */
@WebServlet(name = "OfertaServlet", urlPatterns = "/ms/ofertas/*")
public class OfertaServlet extends ServletBase {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        switch (accion(req)) {
            case "crear": {
                Usuario u = exigir(req, Rol.COMPRADOR);
                Oferta o = c.ofertas.ofertar(u, lng(req, "idInmueble"), monto(req, "monto", "el monto de la oferta"),
                        lngOpc(req, "idGrupo"), txt(req, "condiciones"));
                exito(req, resp, "Oferta registrada. El propietario fue notificado.", "/api/negociaciones/ver?id=" + o.getIdNegociacion());
                break;
            }
            case "retirar": {
                Usuario u = exigir(req, Rol.COMPRADOR);
                Oferta o = c.ofertas.requerir(lng(req, "id"));
                c.ofertas.retirar(u, o.getId());
                exito(req, resp, "Oferta retirada.", "/api/negociaciones/ver?id=" + o.getIdNegociacion());
                break;
            }
            case "aceptar": {
                Usuario u = exigir(req, Rol.PROPIETARIO);
                Oferta o = c.ofertas.requerir(lng(req, "id"));
                c.ofertas.aceptar(u, o.getId());
                exito(req, resp, "Oferta aceptada. La negociación pasó a formalización.", "/api/negociaciones/ver?id=" + o.getIdNegociacion());
                break;
            }
            case "rechazar": {
                Usuario u = exigir(req, Rol.PROPIETARIO);
                Oferta o = c.ofertas.requerir(lng(req, "id"));
                c.ofertas.rechazar(u, o.getId(), txt(req, "motivo"));
                exito(req, resp, "Oferta rechazada y comprador notificado.", "/api/negociaciones/ver?id=" + o.getIdNegociacion());
                break;
            }
            default:
                throw noEncontrado();
        }
    }
}
