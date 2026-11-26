package com.inmocolab.usuarios.servicio;

import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Util;
import com.inmocolab.usuarios.modelo.Administrador;
import com.inmocolab.usuarios.modelo.Asesor;
import com.inmocolab.usuarios.modelo.Bitacora;
import com.inmocolab.usuarios.modelo.Comprador;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Notificacion;
import com.inmocolab.usuarios.modelo.Parametro;
import com.inmocolab.usuarios.modelo.Propietario;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import com.inmocolab.usuarios.persistencia.BitacoraRepositorio;
import com.inmocolab.usuarios.persistencia.NotificacionRepositorio;
import com.inmocolab.usuarios.persistencia.ParametroRepositorio;
import com.inmocolab.usuarios.persistencia.UsuarioRepositorio;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cuentas, perfil, notificaciones, parámetros y bitácora. */
public class UsuarioServicio {

    private final UsuarioRepositorio usuarios;
    private final NotificacionRepositorio notificaciones;
    private final BitacoraRepositorio bitacora;
    private final ParametroRepositorio parametros;
    /** token de recuperación -> {idUsuario, expiraEnMillis}. */
    private final Map<String, long[]> recuperaciones = new ConcurrentHashMap<>();

    public UsuarioServicio(UsuarioRepositorio usuarios, NotificacionRepositorio notificaciones,
            BitacoraRepositorio bitacora, ParametroRepositorio parametros) {
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.bitacora = bitacora;
        this.parametros = parametros;
    }

    // ------------------------------------------------------------ cuenta (CU-01)

    public Usuario registrar(Rol rol, String nombre, String documento, String correo, String telefono,
            String clave, String confirmacion) {
        if (rol != Rol.PROPIETARIO && rol != Rol.COMPRADOR) {
            throw new NegocioException("Solo puedes registrarte como propietario o comprador.");
        }
        if (Util.vacio(nombre) || Util.vacio(documento) || Util.vacio(correo) || Util.vacio(telefono) || Util.vacio(clave)) {
            throw new NegocioException("Nombre, documento, correo, teléfono y contraseña son obligatorios.");
        }
        validarCorreo(correo);
        if (nombre.trim().length() > 100 || documento.trim().length() > 20 || telefono.trim().length() > 20) {
            throw new NegocioException("Nombre (máx. 100), documento (máx. 20) o teléfono (máx. 20) exceden la longitud permitida.");
        }
        AutenticacionServicio.validarPolitica(clave);
        if (!clave.equals(confirmacion)) {
            throw new NegocioException("Las contraseñas no coinciden.");
        }
        if (usuarios.porCorreo(correo.trim()) != null) {
            throw new NegocioException("Ya existe una cuenta con ese correo.");
        }
        if (usuarios.porDocumento(documento.trim()) != null) {
            throw new NegocioException("Ya existe una cuenta con ese documento.");
        }
        Usuario u = crear(rol);
        u.setNombre(nombre.trim());
        u.setDocumento(documento.trim());
        u.setCorreo(correo.trim());
        u.setTelefono(telefono.trim());
        u.setContrasenaHash(AutenticacionServicio.cifrar(clave));
        u.setEstado(EstadoUsuario.PENDIENTE);
        u.setFechaRegistro(LocalDateTime.now());
        u.setTokenVerificacion(UUID.randomUUID().toString());
        usuarios.guardar(u);
        registrarBitacora(u.getId(), "REGISTRO", "Cuenta creada con rol " + rol);
        return u;
    }

    /** Crea una cuenta ya activa (uso interno: datos de ejemplo). */
    public Usuario crearActivo(Rol rol, String nombre, String documento, String correo, String telefono, String clave) {
        Usuario u = crear(rol);
        u.setNombre(nombre);
        u.setDocumento(documento);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setContrasenaHash(AutenticacionServicio.cifrar(clave));
        u.setEstado(EstadoUsuario.ACTIVO);
        u.setFechaRegistro(LocalDateTime.now());
        return usuarios.guardar(u);
    }

    private Usuario crear(Rol rol) {
        switch (rol) {
            case ADMINISTRADOR: return new Administrador();
            case PROPIETARIO: return new Propietario();
            case ASESOR: return new Asesor();
            default: return new Comprador();
        }
    }

    public Usuario verificarCorreo(String token) {
        Usuario u = usuarios.porToken(token);
        if (u == null) {
            throw new NegocioException("El enlace de verificación no es válido o ya fue usado.");
        }
        u.setEstado(EstadoUsuario.ACTIVO);
        u.setTokenVerificacion(null);
        registrarBitacora(u.getId(), "VERIFICACION_CORREO", "Correo verificado");
        notificar(u.getId(), "¡Bienvenido a InmoColab! Tu cuenta está activa.", null);
        return u;
    }

    /** Devuelve el token de verificación si el correo cambió (debe volver a verificarse), o null. */
    public String actualizarPerfil(Usuario u, String telefono, String correo) {
        if (Util.vacio(telefono) || Util.vacio(correo)) {
            throw new NegocioException("Teléfono y correo son obligatorios.");
        }
        validarCorreo(correo);
        if (telefono.trim().length() > 20) {
            throw new NegocioException("El teléfono admite máximo 20 caracteres.");
        }
        String nuevoCorreo = correo.trim();
        String token = null;
        if (!nuevoCorreo.equalsIgnoreCase(u.getCorreo())) {
            if (usuarios.porCorreo(nuevoCorreo) != null) {
                throw new NegocioException("Ya existe una cuenta con ese correo.");
            }
            u.setCorreo(nuevoCorreo);
            u.setEstado(EstadoUsuario.PENDIENTE);
            token = UUID.randomUUID().toString();
            u.setTokenVerificacion(token);
        }
        u.setTelefono(telefono.trim());
        registrarBitacora(u.getId(), "ACTUALIZAR_PERFIL", token == null ? "Datos de contacto" : "Cambio de correo (requiere verificación)");
        return token;
    }

    /** Devuelve el token de recuperación o null si el correo no existe (no se revela al usuario). */
    public String solicitarRecuperacion(String correo) {
        if (Util.vacio(correo)) {
            throw new NegocioException("Ingresa tu correo.");
        }
        Usuario u = usuarios.porCorreo(correo.trim());
        if (u == null) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        recuperaciones.put(token, new long[]{u.getId(), System.currentTimeMillis() + 24L * 3600 * 1000});
        registrarBitacora(u.getId(), "RECUPERAR_CLAVE", "Solicitud de recuperación");
        return token;
    }

    public boolean tokenRecuperacionValido(String token) {
        long[] r = token == null ? null : recuperaciones.get(token);
        return r != null && r[1] > System.currentTimeMillis();
    }

    public void restablecer(String token, String clave, String confirmacion) {
        if (!tokenRecuperacionValido(token)) {
            throw new NegocioException("El enlace de recuperación venció o no es válido.");
        }
        AutenticacionServicio.validarPolitica(clave);
        if (!clave.equals(confirmacion)) {
            throw new NegocioException("Las contraseñas no coinciden.");
        }
        Usuario u = usuarios.buscar(recuperaciones.get(token)[0]);
        u.setContrasenaHash(AutenticacionServicio.cifrar(clave));
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        recuperaciones.remove(token);
        registrarBitacora(u.getId(), "RESTABLECER_CLAVE", "Contraseña restablecida");
    }

    // ------------------------------------------------------------ administración (CU-08)

    public void cambiarEstado(Usuario admin, long idUsuario, EstadoUsuario nuevo, String motivo) {
        Usuario u = usuarios.buscar(idUsuario);
        if (u == null) {
            throw new NegocioException("El usuario no existe.");
        }
        if (nuevo == EstadoUsuario.PENDIENTE) {
            throw new NegocioException("Estado no permitido.");
        }
        if (u.getId() == admin.getId()) {
            throw new NegocioException("No puedes cambiar el estado de tu propia cuenta.");
        }
        if (Util.vacio(motivo)) {
            throw new NegocioException("Toda acción sobre una cuenta exige un motivo.");
        }
        u.setEstado(nuevo);
        u.setBloqueadoHasta(null);
        u.setIntentosFallidos(0);
        registrarBitacora(admin.getId(), "CAMBIO_ESTADO_CUENTA", "Usuario " + u.getCorreo() + " -> " + nuevo + ". Motivo: " + motivo.trim());
        notificar(u.getId(), "Tu cuenta quedó en estado " + Util.etiqueta(nuevo) + ". Motivo: " + motivo.trim(), null);
    }

    public Parametro parametro(String clave) {
        return parametros.buscar(clave);
    }

    public String valorParametro(String clave, String defecto) {
        Parametro p = parametros.buscar(clave);
        return p == null ? defecto : p.getValor();
    }

    public int enteroParametro(String clave, int defecto) {
        try {
            return Integer.parseInt(valorParametro(clave, String.valueOf(defecto)).trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    public List<String> listaParametro(String clave) {
        List<String> lista = new ArrayList<>();
        for (String s : valorParametro(clave, "").split(",")) {
            if (!s.trim().isEmpty()) {
                lista.add(s.trim());
            }
        }
        return lista;
    }

    public void definirParametro(Usuario admin, String clave, String valor) {
        Parametro p = parametros.buscar(clave);
        if (p == null) {
            throw new NegocioException("El parámetro no existe.");
        }
        if (Util.vacio(valor)) {
            throw new NegocioException("El valor no puede estar vacío.");
        }
        if (clave.startsWith("plazo") || clave.startsWith("vigencia")) {
            try {
                if (Integer.parseInt(valor.trim()) <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new NegocioException("Este parámetro debe ser un número entero mayor que cero.");
            }
        }
        String anterior = p.getValor();
        p.setValor(valor.trim());
        registrarBitacora(admin.getId(), "CONFIGURAR_PARAMETRO", clave + ": '" + anterior + "' -> '" + valor.trim() + "'");
    }

    public void crearParametro(String clave, String valor, String descripcion) {
        parametros.guardar(new Parametro(clave, valor, descripcion));
    }

    public List<Parametro> parametros() {
        return parametros.todos();
    }

    // ------------------------------------------------------------ notificaciones (CU-01.4)

    public void notificar(long idUsuario, String mensaje, String enlace) {
        Notificacion n = new Notificacion();
        n.setIdUsuario(idUsuario);
        n.setMensaje(mensaje);
        n.setEnlace(enlace);
        n.setFecha(LocalDateTime.now());
        n.setLeida(false);
        notificaciones.guardar(n);
    }

    public void notificarAdministradores(String mensaje, String enlace) {
        for (Usuario a : usuarios.porRol(Rol.ADMINISTRADOR)) {
            notificar(a.getId(), mensaje, enlace);
        }
    }

    public List<Notificacion> notificaciones(long idUsuario) {
        return notificaciones.porUsuario(idUsuario);
    }

    public int noLeidas(long idUsuario) {
        return notificaciones.noLeidas(idUsuario);
    }

    /** Marca como leída y devuelve el enlace asociado (puede ser null). */
    public String abrirNotificacion(long idUsuario, long idNotificacion) {
        Notificacion n = notificaciones.buscar(idNotificacion);
        if (n == null || n.getIdUsuario() != idUsuario) {
            throw new NegocioException("La notificación no existe.");
        }
        n.setLeida(true);
        return n.getEnlace();
    }

    public void marcarTodasLeidas(long idUsuario) {
        for (Notificacion n : notificaciones.porUsuario(idUsuario)) {
            n.setLeida(true);
        }
    }

    // ------------------------------------------------------------ bitácora (CU-08.6)

    public void registrarBitacora(long idUsuario, String accion, String detalle) {
        bitacora.registrar(idUsuario, accion, detalle);
    }

    public List<Bitacora> bitacora(Long idUsuario, String accion, LocalDate desde, LocalDate hasta) {
        List<Bitacora> resultado = new ArrayList<>();
        for (Bitacora b : bitacora.todas()) {
            if (idUsuario != null && b.getIdUsuario() != idUsuario) {
                continue;
            }
            if (!Util.vacio(accion) && !b.getAccion().equalsIgnoreCase(accion.trim())) {
                continue;
            }
            LocalDate dia = b.getFecha().toLocalDate();
            if ((desde != null && dia.isBefore(desde)) || (hasta != null && dia.isAfter(hasta))) {
                continue;
            }
            resultado.add(b);
        }
        return resultado;
    }

    // ------------------------------------------------------------ consultas

    public Usuario buscar(long id) {
        return usuarios.buscar(id);
    }

    public String nombre(Long id) {
        Usuario u = id == null ? null : usuarios.buscar(id);
        return u == null ? "-" : u.getNombre();
    }

    public List<Usuario> todos() {
        return usuarios.todos();
    }

    public List<Usuario> porRol(Rol rol) {
        return usuarios.porRol(rol);
    }

    public Usuario porCorreo(String correo) {
        return usuarios.porCorreo(correo);
    }

    private void validarCorreo(String correo) {
        if (correo.trim().length() > 120 || !correo.trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new NegocioException("El correo no tiene un formato válido.");
        }
    }
}
