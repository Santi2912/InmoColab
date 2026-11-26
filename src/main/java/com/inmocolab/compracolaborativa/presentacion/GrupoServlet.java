package com.inmocolab.compracolaborativa.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Grupos de compra colaborativa (CU-03). */
@WebServlet(name = "GrupoServlet", urlPatterns = "/ms/grupos/*")
public class GrupoServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Usuario u = usuario(req);
        switch (accion(req)) {
            case "ver": {
                GrupoCompra g = c.grupos.requerir(lng(req, "id"));
                req.setAttribute("grupo", g);
                req.setAttribute("inmueble", c.inmuebles.requerir(g.getIdInmueble()));
                req.setAttribute("miParticipacion", c.grupos.participacionActiva(g, u.getId()));
                req.setAttribute("saldo", c.grupos.saldoPendiente(g));
                vista(req, resp, "grupo", "Grupo de compra #" + g.getId());
                break;
            }
            case "mis":
                exigir(req, Rol.COMPRADOR);
                req.setAttribute("grupos", c.grupos.misGrupos(u.getId()));
                vista(req, resp, "misGrupos", "Mis grupos de compra");
                break;
            default:
                throw noEncontrado();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Usuario u = exigir(req, Rol.COMPRADOR);
        switch (accion(req)) {
            case "crear": {
                GrupoCompra g = c.grupos.crear(u, lng(req, "idInmueble"), monto(req, "montoObjetivo", "el monto objetivo"), fecha(req, "fechaLimite"));
                exito(req, resp, "Grupo creado. Ya eres su primer integrante.", "/api/grupos/ver?id=" + g.getId());
                break;
            }
            case "unirse": {
                long id = lng(req, "id");
                c.grupos.unirse(u, id);
                exito(req, resp, "Te uniste al grupo.", "/api/grupos/ver?id=" + id);
                break;
            }
            case "invitar": {
                long id = lng(req, "id");
                c.grupos.invitar(u, id, txt(req, "correo"));
                exito(req, resp, "Invitación enviada; el invitado fue notificado.", "/api/grupos/ver?id=" + id);
                break;
            }
            case "abandonar":
                c.grupos.abandonar(u, lng(req, "id"));
                exito(req, resp, "Abandonaste el grupo. Tus aportes pasaron a estado En devolución.", "/api/grupos/mis");
                break;
            default:
                throw noEncontrado();
        }
    }
}
