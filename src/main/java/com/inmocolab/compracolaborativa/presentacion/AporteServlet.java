package com.inmocolab.compracolaborativa.presentacion;

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

/** Registro de aportes por el comprador y verificación por el administrador (CU-03.4 y CU-08.3). */
@WebServlet(name = "AporteServlet", urlPatterns = "/ms/aportes/*")
public class AporteServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        exigir(req, Rol.ADMINISTRADOR);
        if (!"pendientes".equals(accion(req))) {
            throw noEncontrado();
        }
        Contexto c = Contexto.get();
        req.setAttribute("aportes", c.aportes.pendientes());
        vista(req, resp, "aportesPendientes", "Aportes por verificar");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        switch (accion(req)) {
            case "registrar": {
                Usuario u = exigir(req, Rol.COMPRADOR);
                long idGrupo = lng(req, "idGrupo");
                c.aportes.registrar(u, idGrupo, monto(req, "monto", "el monto"), txt(req, "comprobante"));
                exito(req, resp, "Aporte registrado. Quedó Pendiente de verificación por el administrador.", "/api/grupos/ver?id=" + idGrupo);
                break;
            }
            case "verificar": {
                Usuario admin = exigir(req, Rol.ADMINISTRADOR);
                String decision = txt(req, "decision");
                if (!"confirmar".equals(decision) && !"rechazar".equals(decision)) {
                    throw new NegocioException("Selecciona confirmar o rechazar.");
                }
                c.aportes.verificar(admin, lng(req, "id"), "confirmar".equals(decision), txt(req, "motivo"));
                exito(req, resp, "Aporte " + ("confirmar".equals(decision) ? "confirmado y participación recalculada." : "rechazado."),
                        "/api/aportes/pendientes");
                break;
            }
            default:
                throw noEncontrado();
        }
    }
}
