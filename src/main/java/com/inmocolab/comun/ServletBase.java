package com.inmocolab.comun;

import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Base de los controladores de la capa de presentación. Solo atiende peticiones que llegaron
 * por el API Gateway (forward) y convierte los errores de negocio en mensajes para el usuario.
 *
 * Patrones: Template Method (service() define el esqueleto; cada servlet completa doGet/doPost) y
 * MVC (el servlet es el controlador, el servicio/modelo el dominio y la JSP la vista).
 */
public abstract class ServletBase extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setCharacterEncoding("UTF-8");
        try {
            super.service(req, resp);
        } catch (NegocioException e) {
            fallo(req, resp, e.getMessage());
        }
    }

    private void fallo(HttpServletRequest req, HttpServletResponse resp, String mensaje) throws IOException {
        if (resp.isCommitted()) {
            return;
        }
        Flash.error(req, mensaje);
        String inicio = Sesion.actual(req) == null ? "/api/usuarios/login" : "/api/usuarios/inicio";
        if ("POST".equals(req.getMethod())) {
            Flash.guardarFormulario(req);
            volver(req, resp, inicio);
        } else {
            ir(req, resp, inicio);
        }
    }

    // ------------------------------------------------------------ navegación

    protected String accion(HttpServletRequest req) {
        String p = req.getPathInfo();
        return p == null || p.length() <= 1 ? "" : p.substring(1);
    }

    protected void ir(HttpServletRequest req, HttpServletResponse resp, String ruta) throws IOException {
        resp.sendRedirect(req.getContextPath() + ruta);
    }

    protected void exito(HttpServletRequest req, HttpServletResponse resp, String mensaje, String ruta) throws IOException {
        Flash.exito(req, mensaje);
        ir(req, resp, ruta);
    }

    /** Redirige a la página de la que vino el formulario (solo dentro de esta aplicación). */
    protected void volver(HttpServletRequest req, HttpServletResponse resp, String porDefecto) throws IOException {
        String destino = porDefecto;
        String ref = req.getHeader("Referer");
        String ctx = req.getContextPath();
        if (ref != null) {
            try {
                URI uri = new URI(ref);
                if (req.getServerName().equals(uri.getHost()) && uri.getPath() != null && uri.getPath().startsWith(ctx + "/")) {
                    destino = uri.getPath().substring(ctx.length()) + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                }
            } catch (URISyntaxException e) {
                destino = porDefecto;
            }
        }
        ir(req, resp, destino);
    }

    protected void vista(HttpServletRequest req, HttpServletResponse resp, String vista, String titulo) throws ServletException, IOException {
        req.setAttribute("vista", vista);
        req.setAttribute("titulo", titulo);
        req.getRequestDispatcher("/WEB-INF/vistas/layout.jsp").forward(req, resp);
    }

    // ------------------------------------------------------------ seguridad

    protected Usuario usuario(HttpServletRequest req) {
        Usuario u = Sesion.actual(req);
        if (u == null) {
            throw new NegocioException("Debes iniciar sesión.");
        }
        return u;
    }

    protected Usuario exigir(HttpServletRequest req, Rol... roles) {
        Usuario u = usuario(req);
        for (Rol r : roles) {
            if (u.getRol() == r) {
                return u;
            }
        }
        throw new NegocioException("Tu rol no tiene permiso para esta acción.");
    }

    protected NegocioException noEncontrado() {
        return new NegocioException("La página solicitada no existe.");
    }

    // ------------------------------------------------------------ parámetros

    protected String txt(HttpServletRequest req, String nombre) {
        String v = req.getParameter(nombre);
        return v == null ? "" : v.trim();
    }

    protected long lng(HttpServletRequest req, String nombre) {
        try {
            return Long.parseLong(txt(req, nombre));
        } catch (NumberFormatException e) {
            throw new NegocioException("Falta un dato requerido o no es válido (" + nombre + ").");
        }
    }

    protected Long lngOpc(HttpServletRequest req, String nombre) {
        String v = txt(req, nombre);
        if (v.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new NegocioException("El dato '" + nombre + "' no es válido.");
        }
    }

    protected BigDecimal montoOpc(HttpServletRequest req, String nombre, String etiqueta) {
        String v = txt(req, nombre);
        return v.isEmpty() ? null : Util.monto(v, etiqueta);
    }

    protected BigDecimal monto(HttpServletRequest req, String nombre, String etiqueta) {
        return Util.monto(txt(req, nombre), etiqueta);
    }

    protected LocalDate fecha(HttpServletRequest req, String nombre) {
        String v = txt(req, nombre);
        if (v.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new NegocioException("La fecha '" + v + "' no es válida.");
        }
    }

    protected static byte[] leer(Part parte) throws IOException {
        try (InputStream in = parte.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }
}
