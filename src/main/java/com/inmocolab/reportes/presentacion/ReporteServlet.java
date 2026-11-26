package com.inmocolab.reportes.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.comun.Util;
import com.inmocolab.reportes.modelo.Reporte;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Consulta y exportación de reportes según el rol (CU-07). */
@WebServlet(name = "ReporteServlet", urlPatterns = "/ms/reportes/*")
public class ReporteServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Usuario u = usuario(req);
        Map<String, String> tipos = c.reportes.tiposPara(u.getRol());
        String tipo = txt(req, "tipo");
        if (tipo.isEmpty()) {
            tipo = tipos.keySet().iterator().next();
        }
        LocalDate desde = fecha(req, "desde");
        LocalDate hasta = fecha(req, "hasta");
        Reporte r = c.reportes.generar(u, tipo, desde, hasta, txt(req, "ciudad"), txt(req, "estado"));
        switch (accion(req)) {
            case "ver":
                req.setAttribute("tipos", tipos);
                req.setAttribute("tipoActual", tipo);
                req.setAttribute("reporte", r);
                req.setAttribute("ciudades", c.usuarios.listaParametro("ciudades"));
                req.setAttribute("generado", Util.fecha(java.time.LocalDateTime.now()));
                vista(req, resp, "reportes", "Reportes");
                break;
            case "exportar": {
                String filtros = "Período: " + Util.fecha(desde) + " a " + Util.fecha(hasta)
                        + (txt(req, "ciudad").isEmpty() ? "" : "; ciudad: " + txt(req, "ciudad"))
                        + (txt(req, "estado").isEmpty() ? "" : "; estado: " + txt(req, "estado"));
                String csv = c.exportacion.aCsv(u, r, filtros);
                resp.setContentType("text/csv; charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=\"reporte-" + tipo.toLowerCase() + ".csv\"");
                resp.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
                break;
            }
            default:
                throw noEncontrado();
        }
    }
}
