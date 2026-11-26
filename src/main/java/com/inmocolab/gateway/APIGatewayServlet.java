package com.inmocolab.gateway;

import com.inmocolab.comun.Flash;
import com.inmocolab.comun.Sesion;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * API Gateway: recibe todo /api/*, verifica la sesión y el rol y reenvía la petición al
 * servlet del microservicio correspondiente (por ejemplo /api/inmuebles/buscar -> InmuebleServlet).
 *
 * Patrones: API Gateway (un único punto de entrada hacia los microservicios) y Front Controller.
 */
@WebServlet(name = "APIGatewayServlet", urlPatterns = "/api/*")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 40 * 1024 * 1024)
public class APIGatewayServlet extends HttpServlet {

    /** Servicios destino y roles permitidos (null = cualquier usuario autenticado). */
    private static final Map<String, Set<Rol>> RUTAS = new HashMap<>();

    static {
        RUTAS.put("usuarios", null);
        RUTAS.put("notificaciones", null);
        RUTAS.put("inmuebles", null);
        RUTAS.put("publicaciones", roles(Rol.ADMINISTRADOR));
        RUTAS.put("grupos", roles(Rol.COMPRADOR, Rol.ADMINISTRADOR));
        RUTAS.put("aportes", roles(Rol.COMPRADOR, Rol.ADMINISTRADOR));
        RUTAS.put("negociaciones", null);
        RUTAS.put("ofertas", roles(Rol.COMPRADOR, Rol.PROPIETARIO));
        RUTAS.put("pqr", null);
        RUTAS.put("comentarios", null);
        RUTAS.put("reportes", null);
    }

    private static Set<Rol> roles(Rol... r) {
        return new HashSet<>(Arrays.asList(r));
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ruta = req.getPathInfo();
        if (ruta == null || ruta.length() <= 1) {
            resp.sendRedirect(req.getContextPath() + "/api/usuarios/inicio");
            return;
        }
        String[] partes = ruta.substring(1).split("/", 2);
        String servicio = partes[0];
        String resto = partes.length > 1 ? "/" + partes[1] : "";
        if (!RUTAS.containsKey(servicio)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Servicio desconocido: " + servicio);
            return;
        }
        Set<Rol> permitidos = RUTAS.get(servicio);
        if (permitidos != null) {
            Usuario u = Sesion.actual(req);
            if (u == null || !permitidos.contains(u.getRol())) {
                Flash.error(req, "Tu rol no tiene acceso a esta sección.");
                resp.sendRedirect(req.getContextPath() + "/api/usuarios/inicio");
                return;
            }
        }
        req.getRequestDispatcher("/ms/" + servicio + resto).forward(req, resp);
    }
}
