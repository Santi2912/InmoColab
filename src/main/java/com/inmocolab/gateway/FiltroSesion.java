package com.inmocolab.gateway;

import com.inmocolab.comun.Flash;
import com.inmocolab.comun.Sesion;
import com.inmocolab.comun.bd.Persistencia;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Exige sesión iniciada para todo /api/*, salvo las pantallas públicas de cuenta. Después de cada petición
 * guarda los cambios en la base de datos.
 *
 * Patrón: Intercepting Filter (cadena de filtros de la API de Servlets).
 */
@WebFilter(filterName = "FiltroSesion", urlPatterns = "/api/*")
public class FiltroSesion implements Filter {

    private static final Set<String> PUBLICAS = new HashSet<>(Arrays.asList(
            "/usuarios/login", "/usuarios/registro", "/usuarios/verificar", "/usuarios/recuperar", "/usuarios/restablecer"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        String ruta = req.getPathInfo() == null ? "" : req.getPathInfo();
        if (!PUBLICAS.contains(ruta) && Sesion.actual(req) == null) {
            Flash.error(req, "Inicia sesión para continuar.");
            resp.sendRedirect(req.getContextPath() + "/api/usuarios/login");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // Guarda en PostgreSQL lo que esta petición cambió (las fotos solo se leen).
            if (!ruta.equals("/inmuebles/foto")) {
                Persistencia.sincronizar();
            }
        }
    }
}
