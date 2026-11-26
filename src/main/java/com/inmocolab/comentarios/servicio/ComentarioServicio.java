package com.inmocolab.comentarios.servicio;

import com.inmocolab.comentarios.modelo.Calificacion;
import com.inmocolab.comentarios.modelo.Comentario;
import com.inmocolab.comentarios.modelo.EstadoComentario;
import com.inmocolab.comentarios.modelo.ReporteComentario;
import com.inmocolab.comentarios.persistencia.CalificacionRepositorio;
import com.inmocolab.comentarios.persistencia.ComentarioRepositorio;
import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Util;
import com.inmocolab.inmuebles.modelo.EstadoPublicacion;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.inmuebles.modelo.Publicacion;
import com.inmocolab.negociacion.modelo.EstadoNegociacion;
import com.inmocolab.negociacion.modelo.Negociacion;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Comentarios sobre inmuebles, reportes y calificaciones (CU-06). */
public class ComentarioServicio {

    public static final int MAX_CARACTERES = 500;
    public static final int REPORTES_PARA_OCULTAR = 3;
    private static final int HORAS_EDICION = 24;

    private final ComentarioRepositorio comentarios;
    private final CalificacionRepositorio calificaciones;

    public ComentarioServicio(ComentarioRepositorio comentarios, CalificacionRepositorio calificaciones) {
        this.comentarios = comentarios;
        this.calificaciones = calificaciones;
    }

    public Comentario publicar(Usuario u, long idInmueble, String texto) {
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(idInmueble);
        Publicacion pub = c.inmuebles.publicacionDe(idInmueble);
        if (pub == null || pub.getEstado() != EstadoPublicacion.APROBADA) {
            throw new NegocioException("Solo se comenta en publicaciones aprobadas.");
        }
        validarContenido(texto);
        Comentario cm = new Comentario();
        cm.setIdInmueble(idInmueble);
        cm.setIdAutor(u.getId());
        cm.setTexto(texto.trim());
        cm.setFecha(LocalDateTime.now());
        cm.setEstado(EstadoComentario.VISIBLE);
        comentarios.guardar(cm);
        if (i.getIdPropietario() != u.getId()) {
            c.usuarios.notificar(i.getIdPropietario(), u.getNombre() + " comentó en " + i.getDireccion(),
                    "/api/inmuebles/detalle?id=" + i.getId() + "#comentarios");
        }
        return cm;
    }

    public void responder(Usuario u, long idComentario, String texto) {
        Comentario cm = requerir(idComentario);
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(cm.getIdInmueble());
        if (u.getRol() != Rol.PROPIETARIO || i.getIdPropietario() != u.getId()) {
            throw new NegocioException("Solo el propietario responde comentarios de sus propios inmuebles.");
        }
        if (cm.getRespuesta() != null) {
            throw new NegocioException("Este comentario ya tiene respuesta.");
        }
        validarContenido(texto);
        cm.setRespuesta(texto.trim());
        cm.setFechaRespuesta(LocalDateTime.now());
        c.usuarios.notificar(cm.getIdAutor(), "El propietario respondió tu comentario en " + i.getDireccion(),
                "/api/inmuebles/detalle?id=" + i.getId() + "#comentarios");
    }

    public void editar(Usuario u, long idComentario, String texto) {
        Comentario cm = propio(u, idComentario);
        if (cm.getFecha().plusHours(HORAS_EDICION).isBefore(LocalDateTime.now())) {
            throw new NegocioException("Solo puedes editar un comentario durante las primeras " + HORAS_EDICION + " horas.");
        }
        validarContenido(texto);
        cm.setTexto(texto.trim());
        cm.setEditado(true);
    }

    public long eliminar(Usuario u, long idComentario) {
        Comentario cm = requerir(idComentario);
        if (cm.getIdAutor() != u.getId() && u.getRol() != Rol.ADMINISTRADOR) {
            throw new NegocioException("Solo puedes eliminar tus propios comentarios.");
        }
        comentarios.eliminar(idComentario);
        return cm.getIdInmueble();
    }

    public void reportar(Usuario u, long idComentario, String motivo) {
        Comentario cm = requerir(idComentario);
        if (cm.getIdAutor() == u.getId()) {
            throw new NegocioException("No puedes reportar tu propio comentario.");
        }
        if (Util.vacio(motivo)) {
            throw new NegocioException("Selecciona el motivo del reporte.");
        }
        if (cm.isModerado() && cm.getEstado() == EstadoComentario.VISIBLE) {
            throw new NegocioException("Este comentario ya fue revisado por moderación.");
        }
        for (ReporteComentario r : cm.getReportes()) {
            if (r.getIdUsuario() == u.getId()) {
                throw new NegocioException("Ya reportaste este comentario.");
            }
        }
        cm.getReportes().add(new ReporteComentario(u.getId(), motivo.trim(), LocalDateTime.now()));
        Contexto c = Contexto.get();
        if (cm.getReportes().size() >= REPORTES_PARA_OCULTAR && cm.getEstado() == EstadoComentario.VISIBLE) {
            cm.setEstado(EstadoComentario.OCULTO);
            cm.setMotivoOcultar("Ocultado automáticamente por " + REPORTES_PARA_OCULTAR + " reportes, pendiente de revisión.");
            c.usuarios.notificarAdministradores("Un comentario fue ocultado por reportes y espera moderación.", "/api/comentarios/moderar");
            c.usuarios.notificar(cm.getIdAutor(), "Tu comentario fue ocultado temporalmente por reportes de otros usuarios.", null);
        }
    }

    // ------------------------------------------------------------ CU-06.5 Calificar

    public Calificacion calificar(Usuario u, long idNegociacion, int estrellas, String texto) {
        Contexto c = Contexto.get();
        Negociacion n = c.negociaciones.requerir(idNegociacion);
        if (u.getRol() != Rol.COMPRADOR || n.getEstado() != EstadoNegociacion.CERRADA || !c.negociaciones.compradores(n).contains(u.getId())) {
            throw new NegocioException("Solo puedes calificar una negociación cerrada en la que participaste.");
        }
        if (estrellas < 1 || estrellas > 5) {
            throw new NegocioException("La calificación va de 1 a 5 estrellas.");
        }
        if (calificaciones.existe(idNegociacion, u.getId())) {
            throw new NegocioException("Ya calificaste esta negociación.");
        }
        if (!Util.vacio(texto) && texto.trim().length() > MAX_CARACTERES) {
            throw new NegocioException("El comentario admite máximo " + MAX_CARACTERES + " caracteres.");
        }
        Calificacion cal = new Calificacion();
        cal.setIdNegociacion(idNegociacion);
        cal.setIdComprador(u.getId());
        cal.setEstrellas(estrellas);
        cal.setTexto(Util.vacio(texto) ? null : texto.trim());
        cal.setFecha(LocalDateTime.now());
        return calificaciones.guardar(cal);
    }

    public boolean puedeCalificar(Usuario u, Negociacion n) {
        return u.getRol() == Rol.COMPRADOR && n.getEstado() == EstadoNegociacion.CERRADA
                && Contexto.get().negociaciones.compradores(n).contains(u.getId()) && !calificaciones.existe(n.getId(), u.getId());
    }

    public List<Calificacion> calificacionesDe(long idNegociacion) {
        return calificaciones.filtrar(c -> c.getIdNegociacion() == idNegociacion);
    }

    public List<Calificacion> todasCalificaciones() {
        return calificaciones.todos();
    }

    // ------------------------------------------------------------ consultas

    /** Comentarios visibles; el administrador y el autor ven también los ocultos. */
    public List<Comentario> deInmueble(long idInmueble, Usuario viewer) {
        List<Comentario> lista = new ArrayList<>();
        for (Comentario cm : comentarios.porInmueble(idInmueble)) {
            if (cm.getEstado() == EstadoComentario.VISIBLE || viewer.getRol() == Rol.ADMINISTRADOR || cm.getIdAutor() == viewer.getId()) {
                lista.add(cm);
            }
        }
        return lista;
    }

    public boolean puedeEditar(Usuario u, Comentario cm) {
        return cm.getIdAutor() == u.getId() && !cm.getFecha().plusHours(HORAS_EDICION).isBefore(LocalDateTime.now());
    }

    public Comentario requerir(long id) {
        Comentario cm = comentarios.buscar(id);
        if (cm == null) {
            throw new NegocioException("El comentario no existe.");
        }
        return cm;
    }

    public List<Comentario> todos() {
        return comentarios.todos();
    }

    private Comentario propio(Usuario u, long id) {
        Comentario cm = requerir(id);
        if (cm.getIdAutor() != u.getId()) {
            throw new NegocioException("Solo puedes modificar tus propios comentarios.");
        }
        return cm;
    }

    private void validarContenido(String texto) {
        if (Util.vacio(texto)) {
            throw new NegocioException("Escribe el comentario.");
        }
        if (texto.trim().length() > MAX_CARACTERES) {
            throw new NegocioException("El comentario admite máximo " + MAX_CARACTERES + " caracteres (tiene " + texto.trim().length() + ").");
        }
        String minusculas = texto.toLowerCase(Locale.ROOT);
        for (String palabra : Contexto.get().usuarios.listaParametro("palabras_prohibidas")) {
            if (minusculas.contains(palabra.toLowerCase(Locale.ROOT))) {
                throw new NegocioException("El comentario contiene palabras no permitidas ('" + palabra + "').");
            }
        }
    }
}
