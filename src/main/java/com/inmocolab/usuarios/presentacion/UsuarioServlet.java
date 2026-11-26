package com.inmocolab.usuarios.presentacion;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.Flash;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.ServletBase;
import com.inmocolab.comun.Sesion;
import com.inmocolab.comun.Util;
import com.inmocolab.negociacion.modelo.EstadoOferta;
import com.inmocolab.negociacion.modelo.Negociacion;
import com.inmocolab.negociacion.modelo.Oferta;
import com.inmocolab.pqr.modelo.EstadoPqr;
import com.inmocolab.pqr.modelo.PQR;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Cuenta de usuario (CU-01) y administración de cuentas, parámetros y bitácora (CU-08). */
@WebServlet(name = "UsuarioServlet", urlPatterns = "/ms/usuarios/*")
public class UsuarioServlet extends ServletBase {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        switch (accion(req)) {
            case "login":
                if (Sesion.actual(req) != null) {
                    ir(req, resp, "/api/usuarios/inicio");
                    return;
                }
                vista(req, resp, "login", "Iniciar sesión");
                break;
            case "logout":
                Sesion.cerrar(req);
                exito(req, resp, "Sesión cerrada correctamente.", "/api/usuarios/login");
                break;
            case "registro":
                vista(req, resp, "registro", "Crear cuenta");
                break;
            case "verificar":
                c.usuarios.verificarCorreo(txt(req, "token"));
                exito(req, resp, "Correo verificado. Ya puedes iniciar sesión.", "/api/usuarios/login");
                break;
            case "recuperar":
                vista(req, resp, "recuperar", "Recuperar contraseña");
                break;
            case "restablecer":
                if (!c.usuarios.tokenRecuperacionValido(txt(req, "token"))) {
                    throw new NegocioException("El enlace de recuperación venció o no es válido.");
                }
                req.setAttribute("token", txt(req, "token"));
                vista(req, resp, "restablecer", "Nueva contraseña");
                break;
            case "inicio":
                req.setAttribute("resumen", resumen(usuario(req)));
                vista(req, resp, "inicio", "Inicio");
                break;
            case "perfil":
                vista(req, resp, "perfil", "Mi perfil");
                break;
            case "cuentas":
                exigir(req, Rol.ADMINISTRADOR);
                req.setAttribute("usuarios", c.usuarios.todos());
                vista(req, resp, "cuentas", "Cuentas de usuario");
                break;
            case "parametros":
                exigir(req, Rol.ADMINISTRADOR);
                req.setAttribute("parametros", c.usuarios.parametros());
                vista(req, resp, "parametros", "Parámetros del sistema");
                break;
            case "bitacora":
                exigir(req, Rol.ADMINISTRADOR);
                req.setAttribute("registros", c.usuarios.bitacora(lngOpc(req, "idUsuario"), txt(req, "accionFiltro"),
                        fecha(req, "desde"), fecha(req, "hasta")));
                req.setAttribute("usuarios", c.usuarios.todos());
                vista(req, resp, "bitacora", "Bitácora de operaciones");
                break;
            default:
                throw noEncontrado();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Contexto c = Contexto.get();
        switch (accion(req)) {
            case "login": {
                Usuario u = c.autenticacion.iniciarSesion(txt(req, "correo"), req.getParameter("clave"));
                Sesion.iniciar(req, u);
                ir(req, resp, "/api/usuarios/inicio");
                break;
            }
            case "registro": {
                Rol rol;
                try {
                    rol = Rol.valueOf(txt(req, "rol"));
                } catch (IllegalArgumentException e) {
                    throw new NegocioException("Selecciona si eres propietario o comprador.");
                }
                Usuario u = c.usuarios.registrar(rol, txt(req, "nombre"), txt(req, "documento"), txt(req, "correo"),
                        txt(req, "telefono"), req.getParameter("clave"), req.getParameter("confirmacion"));
                mostrarEnlace(req, resp, "Cuenta creada",
                        "Te enviamos un correo para verificar tu cuenta. En este prototipo no hay servidor de correo: usa este enlace.",
                        "/api/usuarios/verificar?token=" + u.getTokenVerificacion());
                break;
            }
            case "recuperar": {
                String token = c.usuarios.solicitarRecuperacion(txt(req, "correo"));
                mostrarEnlace(req, resp, "Revisa tu correo",
                        "Si el correo está registrado, enviamos un enlace de recuperación válido por 24 horas. "
                        + "En este prototipo no hay servidor de correo: " + (token == null ? "ese correo no existe, por eso no se muestra enlace." : "usa este enlace."),
                        token == null ? null : "/api/usuarios/restablecer?token=" + token);
                break;
            }
            case "restablecer":
                c.usuarios.restablecer(txt(req, "token"), req.getParameter("clave"), req.getParameter("confirmacion"));
                exito(req, resp, "Contraseña actualizada. Ya puedes iniciar sesión.", "/api/usuarios/login");
                break;
            case "perfil": {
                Usuario u = usuario(req);
                String token = c.usuarios.actualizarPerfil(u, txt(req, "telefono"), txt(req, "correo"));
                if (token != null) {
                    Sesion.cerrar(req);
                    mostrarEnlace(req, resp, "Verifica tu nuevo correo",
                            "El cambio de correo exige una nueva verificación. Tu sesión se cerró; usa este enlace para reactivar la cuenta.",
                            "/api/usuarios/verificar?token=" + token);
                } else {
                    exito(req, resp, "Perfil actualizado.", "/api/usuarios/perfil");
                }
                break;
            }
            case "estado": {
                Usuario admin = exigir(req, Rol.ADMINISTRADOR);
                EstadoUsuario estado;
                try {
                    estado = EstadoUsuario.valueOf(txt(req, "estado"));
                } catch (IllegalArgumentException e) {
                    throw new NegocioException("Estado no válido.");
                }
                c.usuarios.cambiarEstado(admin, lng(req, "idUsuario"), estado, txt(req, "motivo"));
                exito(req, resp, "Cuenta actualizada y usuario notificado.", "/api/usuarios/cuentas");
                break;
            }
            case "parametro": {
                Usuario admin = exigir(req, Rol.ADMINISTRADOR);
                c.usuarios.definirParametro(admin, txt(req, "clave"), txt(req, "valor"));
                exito(req, resp, "Parámetro actualizado.", "/api/usuarios/parametros");
                break;
            }
            default:
                throw noEncontrado();
        }
    }

    private void mostrarEnlace(HttpServletRequest req, HttpServletResponse resp, String titulo, String mensaje, String enlace)
            throws ServletException, IOException {
        Flash.limpiarFormulario(req);
        req.setAttribute("mensajeEnlace", mensaje);
        req.setAttribute("enlace", enlace);
        vista(req, resp, "enlace", titulo);
    }

    /** Tarjetas del panel de inicio: etiqueta -> {valor, enlace}. */
    private Map<String, String[]> resumen(Usuario u) {
        Contexto c = Contexto.get();
        Map<String, String[]> r = new LinkedHashMap<>();
        String noLeidas = String.valueOf(c.usuarios.noLeidas(u.getId()));
        switch (u.getRol()) {
            case ADMINISTRADOR: {
                r.put("Publicaciones por validar", new String[]{String.valueOf(c.publicaciones.pendientes().size()), "/api/publicaciones/pendientes"});
                r.put("Aportes por verificar", new String[]{String.valueOf(c.aportes.pendientes().size()), "/api/aportes/pendientes"});
                int abiertas = 0;
                for (PQR p : c.pqr.todas()) {
                    if (p.getEstado() == EstadoPqr.RADICADA || p.getEstado() == EstadoPqr.EN_TRAMITE) {
                        abiertas++;
                    }
                }
                r.put("PQR sin responder", new String[]{String.valueOf(abiertas), "/api/pqr/lista"});
                r.put("Comentarios por moderar", new String[]{String.valueOf(c.moderacion.reportados().size()), "/api/comentarios/moderar"});
                r.put("Usuarios registrados", new String[]{String.valueOf(c.usuarios.todos().size()), "/api/usuarios/cuentas"});
                break;
            }
            case PROPIETARIO: {
                int pendientes = 0;
                for (Negociacion n : c.negociaciones.misNegociaciones(u)) {
                    for (Oferta o : n.getOfertas()) {
                        pendientes += o.getEstado() == EstadoOferta.PENDIENTE ? 1 : 0;
                    }
                }
                r.put("Mis inmuebles", new String[]{String.valueOf(c.inmuebles.misInmuebles(u.getId()).size()), "/api/inmuebles/mis"});
                r.put("Ofertas por responder", new String[]{String.valueOf(pendientes), "/api/negociaciones/mis"});
                break;
            }
            case COMPRADOR:
                r.put("Mis grupos de compra", new String[]{String.valueOf(c.grupos.misGrupos(u.getId()).size()), "/api/grupos/mis"});
                r.put("Mis negociaciones", new String[]{String.valueOf(c.negociaciones.misNegociaciones(u).size()), "/api/negociaciones/mis"});
                break;
            default:
                r.put("Negociaciones asignadas", new String[]{String.valueOf(c.negociaciones.misNegociaciones(u).size()), "/api/negociaciones/mis"});
        }
        r.put("PQR", new String[]{String.valueOf(c.pqr.listar(u).size()), "/api/pqr/lista"});
        r.put("Notificaciones sin leer", new String[]{noLeidas, "/api/notificaciones/lista"});
        return r;
    }
}
