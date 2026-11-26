package com.inmocolab.inmuebles.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.Flash;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Pagina;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.comun.Util;
import com.inmocolab.compracolaborativa.modelo.EstadoGrupo;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.inmuebles.modelo.Fotografia;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.inmuebles.servicio.FiltroInmueble;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/** Gestión de inmuebles (CU-02): búsqueda, detalle, registro, edición y retiro. */
@WebServlet(name = "InmuebleServlet", urlPatterns = "/ms/inmuebles/*")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 40 * 1024 * 1024)
public class InmuebleServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Usuario u = usuario(req);
        switch (accion(req)) {
            case "buscar":
                buscar(req, resp);
                break;
            case "detalle":
                detalle(req, resp, u);
                break;
            case "mis":
                if (u.getRol() == Rol.ADMINISTRADOR) {
                    req.setAttribute("inmuebles", c.inmuebles.todos());
                } else {
                    exigir(req, Rol.PROPIETARIO);
                    req.setAttribute("inmuebles", c.inmuebles.misInmuebles(u.getId()));
                }
                vista(req, resp, "misInmuebles", u.getRol() == Rol.ADMINISTRADOR ? "Todos los inmuebles" : "Mis inmuebles");
                break;
            case "registrar":
                exigir(req, Rol.PROPIETARIO);
                catalogos(req);
                vista(req, resp, "registrarInmueble", "Publicar inmueble");
                break;
            case "editar": {
                exigir(req, Rol.PROPIETARIO);
                Inmueble i = c.inmuebles.requerir(lng(req, "id"));
                if (i.getIdPropietario() != u.getId()) {
                    throw new NegocioException("Solo puedes editar tus propios inmuebles.");
                }
                catalogos(req);
                req.setAttribute("inmueble", i);
                vista(req, resp, "registrarInmueble", "Editar inmueble");
                break;
            }
            case "foto":
                foto(req, resp);
                break;
            default:
                throw noEncontrado();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        switch (accion(req)) {
            case "registrar": {
                Usuario u = exigir(req, Rol.PROPIETARIO);
                List<Fotografia> fotos = fotosDe(req);
                Inmueble i = c.inmuebles.registrar(u, txt(req, "direccion"), txt(req, "ciudad"), txt(req, "tipo"),
                        monto(req, "precio", "el precio"), monto(req, "area", "el área"), txt(req, "descripcion"),
                        montoOpc(req, "precioMinimo", "la oferta mínima"), fotos);
                exito(req, resp, "Inmueble registrado. Quedó en estado Pendiente de validación.", "/api/inmuebles/detalle?id=" + i.getId());
                break;
            }
            case "editar": {
                Usuario u = exigir(req, Rol.PROPIETARIO);
                long id = lng(req, "id");
                c.inmuebles.editar(u, id, txt(req, "direccion"), txt(req, "ciudad"), txt(req, "tipo"), monto(req, "precio", "el precio"),
                        monto(req, "area", "el área"), txt(req, "descripcion"), montoOpc(req, "precioMinimo", "la oferta mínima"));
                exito(req, resp, "Inmueble actualizado.", "/api/inmuebles/detalle?id=" + id);
                break;
            }
            case "retirar": {
                Usuario u = exigir(req, Rol.PROPIETARIO);
                c.inmuebles.retirar(u, lng(req, "id"));
                exito(req, resp, "La publicación fue retirada y ya no aparece en las búsquedas.", "/api/inmuebles/mis");
                break;
            }
            case "asignarAsesor": {
                Usuario admin = exigir(req, Rol.ADMINISTRADOR);
                long id = lng(req, "id");
                c.inmuebles.asignarAsesor(admin, id, lng(req, "idAsesor"));
                exito(req, resp, "Asesor asignado y notificado.", "/api/inmuebles/detalle?id=" + id);
                break;
            }
            default:
                throw noEncontrado();
        }
    }

    // ------------------------------------------------------------ búsqueda (HU-09, HU-10)

    private void buscar(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        FiltroInmueble f = new FiltroInmueble();
        f.palabra = txt(req, "q");
        f.ciudad = txt(req, "ciudad");
        f.tipo = txt(req, "tipo");
        Pagina<Inmueble> resultado;
        try {
            f.precioMin = montoOpc(req, "precioMin", "el precio mínimo");
            f.precioMax = montoOpc(req, "precioMax", "el precio máximo");
            f.areaMin = montoOpc(req, "areaMin", "el área mínima");
            f.areaMax = montoOpc(req, "areaMax", "el área máxima");
            try {
                f.pagina = Integer.parseInt(txt(req, "pagina"));
            } catch (NumberFormatException e) {
                f.pagina = 1;
            }
            resultado = c.inmuebles.buscar(f);
        } catch (NegocioException e) {
            Flash.error(req, e.getMessage());
            resultado = new Pagina<>(new ArrayList<Inmueble>(), 1, 12);
        }
        StringBuilder qs = new StringBuilder();
        for (String p : new String[]{"q", "ciudad", "tipo", "precioMin", "precioMax", "areaMin", "areaMax"}) {
            if (!txt(req, p).isEmpty()) {
                qs.append('&').append(p).append('=').append(java.net.URLEncoder.encode(txt(req, p), "UTF-8"));
            }
        }
        catalogos(req);
        req.setAttribute("resultado", resultado);
        req.setAttribute("consulta", qs.toString());
        vista(req, resp, "buscar", "Buscar inmuebles");
    }

    // ------------------------------------------------------------ detalle (HU-11)

    private void detalle(HttpServletRequest req, HttpServletResponse resp, Usuario u) throws ServletException, IOException {
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.detalle(lng(req, "id"), u);
        req.setAttribute("inmueble", i);
        req.setAttribute("publicacion", c.inmuebles.publicacionDe(i.getId()));
        req.setAttribute("gruposAbiertos", c.grupos.abiertosPorInmueble(i.getId()));
        req.setAttribute("comentarios", c.comentarios.deInmueble(i.getId(), u));
        req.setAttribute("interesados", c.negociaciones.cantidadInteresados(i.getId()));
        req.setAttribute("yaInteresado", c.negociaciones.tieneInteres(i.getId(), u.getId()));
        req.setAttribute("negociacion", c.negociaciones.vigente(i.getId()));
        List<GrupoCompra> listos = new ArrayList<>();
        if (u.getRol() == Rol.COMPRADOR) {
            for (GrupoCompra g : c.grupos.misGrupos(u.getId())) {
                if (g.getIdInmueble() == i.getId() && g.getEstado() == EstadoGrupo.COMPLETO) {
                    listos.add(g);
                }
            }
            req.setAttribute("misGruposListos", listos);
        }
        if (u.getRol() == Rol.ADMINISTRADOR) {
            req.setAttribute("asesores", c.usuarios.porRol(Rol.ASESOR));
        }
        vista(req, resp, "detalle", "Detalle del inmueble");
    }

    // ------------------------------------------------------------ fotografías

    private List<Fotografia> fotosDe(HttpServletRequest req) throws IOException, ServletException {
        List<Fotografia> fotos = new ArrayList<>();
        try {
            for (Part p : req.getParts()) {
                if ("fotos".equals(p.getName()) && p.getSize() > 0 && !Util.vacio(p.getSubmittedFileName())) {
                    Fotografia f = new Fotografia();
                    f.setNombre(p.getSubmittedFileName());
                    f.setContentType(p.getContentType());
                    f.setDatos(leer(p));
                    fotos.add(f);
                }
            }
        } catch (IllegalStateException e) {
            throw new NegocioException("Una imagen supera 5 MB o el envío es demasiado grande.");
        }
        return fotos;
    }

    private void foto(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Fotografia f = Contexto.get().inmuebles.fotografia(lng(req, "id"));
        if (f == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setHeader("Cache-Control", "private, max-age=3600");
        if (f.getDatos() != null) {
            resp.setContentType(f.getContentType());
            resp.getOutputStream().write(f.getDatos());
            return;
        }
        // Foto de ejemplo: ilustración generada con un color propio por inmueble.
        String[][] paletas = {{"#2f6f73", "#cfe7e8"}, {"#a15c38", "#f2dccd"}, {"#4b5d8f", "#d9def0"}, {"#6b7d3a", "#e4ebcb"}, {"#8f4b6b", "#f0d9e5"}};
        String[] p = paletas[(int) (f.getId() % paletas.length)];
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 260'><rect width='400' height='260' fill='" + p[1] + "'/>"
                + "<rect x='0' y='190' width='400' height='70' fill='" + p[0] + "' opacity='.25'/>"
                + "<polygon points='200,50 310,130 90,130' fill='" + p[0] + "'/><rect x='110' y='130' width='180' height='85' fill='#fff' stroke='" + p[0] + "' stroke-width='4'/>"
                + "<rect x='185' y='160' width='30' height='55' fill='" + p[0] + "'/><rect x='130' y='150' width='38' height='32' fill='" + p[1] + "' stroke='" + p[0] + "' stroke-width='3'/>"
                + "<rect x='232' y='150' width='38' height='32' fill='" + p[1] + "' stroke='" + p[0] + "' stroke-width='3'/>"
                + "<text x='200' y='245' font-family='sans-serif' font-size='16' fill='" + p[0] + "' text-anchor='middle'>Foto " + f.getOrden() + " (ejemplo)</text></svg>";
        resp.setContentType("image/svg+xml");
        resp.getOutputStream().write(svg.getBytes(StandardCharsets.UTF_8));
    }

    private void catalogos(HttpServletRequest req) {
        req.setAttribute("ciudades", Contexto.get().usuarios.listaParametro("ciudades"));
        req.setAttribute("tipos", Contexto.get().usuarios.listaParametro("tipos_inmueble"));
    }
}
