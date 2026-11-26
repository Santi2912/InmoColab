package com.inmocolab.pqr.servicio;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Secuencia;
import com.inmocolab.comun.Util;
import com.inmocolab.pqr.modelo.Adjunto;
import com.inmocolab.pqr.modelo.EstadoPqr;
import com.inmocolab.pqr.modelo.PQR;
import com.inmocolab.pqr.modelo.RespuestaPQR;
import com.inmocolab.pqr.modelo.TipoPqr;
import com.inmocolab.pqr.persistencia.PqrRepositorio;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Radicación, atención y cierre de peticiones, quejas y reclamos (CU-05). */
public class PqrServicio {

    public static final int MAX_ADJUNTOS = 3;
    public static final long MAX_BYTES_ADJUNTO = 5L * 1024 * 1024;
    private static final int DIAS_REABRIR = 5;
    private static final int DIAS_ALERTA = 3;

    private final PqrRepositorio pqrs;

    public PqrServicio(PqrRepositorio pqrs) {
        this.pqrs = pqrs;
    }

    // ------------------------------------------------------------ CU-05.1 Radicar

    public PQR radicar(Usuario u, TipoPqr tipo, String asunto, String descripcion, Long idInmueble, Long idNegociacion,
            List<Adjunto> adjuntos) {
        if (tipo == null || Util.vacio(asunto) || Util.vacio(descripcion)) {
            throw new NegocioException("Tipo, asunto y descripción son obligatorios.");
        }
        if (asunto.trim().length() > 120) {
            throw new NegocioException("El asunto admite máximo 120 caracteres.");
        }
        validarAdjuntos(adjuntos);
        Contexto c = Contexto.get();
        if (idInmueble != null && c.inmuebles.buscar(idInmueble) == null) {
            throw new NegocioException("El inmueble relacionado no existe.");
        }
        if (idNegociacion != null) {
            c.negociaciones.ver(u, idNegociacion);
        }
        PQR p = new PQR();
        p.setRadicado(String.format("PQR-%d-%04d", LocalDate.now().getYear(), pqrs.todos().size() + 1));
        p.setIdUsuario(u.getId());
        p.setTipo(tipo);
        p.setAsunto(asunto.trim());
        p.setDescripcion(descripcion.trim());
        p.setIdInmueble(idInmueble);
        p.setIdNegociacion(idNegociacion);
        p.setFechaRadicacion(LocalDateTime.now());
        p.setFechaLimite(sumarDiasHabiles(LocalDate.now(), c.usuarios.enteroParametro("plazo_pqr_dias", 15)));
        p.setEstado(EstadoPqr.RADICADA);
        if (adjuntos != null) {
            p.getAdjuntos().addAll(adjuntos);
        }
        pqrs.guardar(p);
        c.usuarios.registrarBitacora(u.getId(), "RADICAR_PQR", p.getRadicado());
        c.usuarios.notificarAdministradores("Nueva " + Util.etiqueta(tipo).toLowerCase(Locale.ROOT) + " radicada: " + p.getRadicado(),
                "/api/pqr/ver?id=" + p.getId());
        return p;
    }

    public static void validarAdjuntos(List<Adjunto> adjuntos) {
        if (adjuntos == null) {
            return;
        }
        if (adjuntos.size() > MAX_ADJUNTOS) {
            throw new NegocioException("Puedes adjuntar máximo " + MAX_ADJUNTOS + " archivos.");
        }
        for (Adjunto a : adjuntos) {
            String n = a.getNombre().toLowerCase(Locale.ROOT);
            if (!(n.endsWith(".pdf") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png"))) {
                throw new NegocioException("El adjunto '" + a.getNombre() + "' debe ser PDF, JPG o PNG.");
            }
            if (a.getTamano() > MAX_BYTES_ADJUNTO) {
                throw new NegocioException("El adjunto '" + a.getNombre() + "' supera 5 MB.");
            }
        }
    }

    public static LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        LocalDate f = desde;
        int agregados = 0;
        while (agregados < dias) {
            f = f.plusDays(1);
            if (f.getDayOfWeek() != DayOfWeek.SATURDAY && f.getDayOfWeek() != DayOfWeek.SUNDAY) {
                agregados++;
            }
        }
        return f;
    }

    // ------------------------------------------------------------ CU-05.4 Asignar / CU-05.6 Responder

    public void asignar(Usuario admin, long idPqr, long idResponsable) {
        exigirAdmin(admin);
        PQR p = requerir(idPqr);
        Contexto c = Contexto.get();
        Usuario r = c.usuarios.buscar(idResponsable);
        if (r == null || r.getEstado() != EstadoUsuario.ACTIVO || (r.getRol() != Rol.ASESOR && r.getRol() != Rol.ADMINISTRADOR)) {
            throw new NegocioException("El responsable debe ser un asesor activo o un administrador.");
        }
        if (p.getEstado() == EstadoPqr.CERRADA) {
            throw new NegocioException("La PQR ya está cerrada.");
        }
        p.setIdResponsable(r.getId());
        if (p.getEstado() == EstadoPqr.RADICADA) {
            p.setEstado(EstadoPqr.EN_TRAMITE);
        }
        c.usuarios.registrarBitacora(admin.getId(), "ASIGNAR_PQR", p.getRadicado() + " -> " + r.getNombre());
        c.usuarios.notificar(r.getId(), "Se te asignó la PQR " + p.getRadicado(), "/api/pqr/ver?id=" + p.getId());
        c.usuarios.notificar(p.getIdUsuario(), "Tu PQR " + p.getRadicado() + " está en trámite.", "/api/pqr/ver?id=" + p.getId());
    }

    public void responder(Usuario u, long idPqr, String texto) {
        PQR p = requerir(idPqr);
        boolean admin = u.getRol() == Rol.ADMINISTRADOR;
        boolean asignado = u.getRol() == Rol.ASESOR && p.getIdResponsable() != null && p.getIdResponsable() == u.getId();
        if (!admin && !asignado) {
            throw new NegocioException("No tienes permiso para responder esta PQR.");
        }
        if (p.getEstado() == EstadoPqr.CERRADA || p.getEstado() == EstadoPqr.RESPONDIDA) {
            throw new NegocioException("La PQR ya fue respondida.");
        }
        if (Util.vacio(texto)) {
            throw new NegocioException("La respuesta es obligatoria.");
        }
        RespuestaPQR r = new RespuestaPQR();
        r.setId(Secuencia.siguiente());
        r.setIdAutor(u.getId());
        r.setTexto(texto.trim());
        r.setFecha(LocalDateTime.now());
        p.getRespuestas().add(r);
        p.setFechaRespuesta(r.getFecha());
        p.setEstado(EstadoPqr.RESPONDIDA);
        if (p.getIdResponsable() == null) {
            p.setIdResponsable(u.getId());
        }
        Contexto c = Contexto.get();
        c.usuarios.registrarBitacora(u.getId(), "RESPONDER_PQR", p.getRadicado());
        c.usuarios.notificar(p.getIdUsuario(), "Respondieron tu PQR " + p.getRadicado(), "/api/pqr/ver?id=" + p.getId());
    }

    // ------------------------------------------------------------ CU-05.3 Cerrar o reabrir

    public void cerrar(Usuario u, long idPqr, Integer calificacion) {
        PQR p = propia(u, idPqr);
        if (p.getEstado() != EstadoPqr.RESPONDIDA) {
            throw new NegocioException("Solo puedes cerrar una PQR que ya fue respondida.");
        }
        if (calificacion != null && (calificacion < 1 || calificacion > 5)) {
            throw new NegocioException("La calificación va de 1 a 5.");
        }
        p.setEstado(EstadoPqr.CERRADA);
        p.setFechaCierre(LocalDateTime.now());
        p.setCalificacion(calificacion);
        Contexto.get().usuarios.registrarBitacora(u.getId(), "CERRAR_PQR", p.getRadicado());
    }

    public void reabrir(Usuario u, long idPqr) {
        PQR p = propia(u, idPqr);
        if (p.getEstado() != EstadoPqr.RESPONDIDA && p.getEstado() != EstadoPqr.CERRADA) {
            throw new NegocioException("Solo puedes reabrir una PQR respondida o cerrada.");
        }
        if (p.isReabierta()) {
            throw new NegocioException("Esta PQR ya fue reabierta una vez.");
        }
        LocalDateTime referencia = p.getFechaCierre() != null ? p.getFechaCierre() : p.getFechaRespuesta();
        if (referencia != null && referencia.plusDays(DIAS_REABRIR).isBefore(LocalDateTime.now())) {
            throw new NegocioException("Solo puedes reabrirla dentro de los " + DIAS_REABRIR + " días siguientes a la respuesta.");
        }
        p.setReabierta(true);
        p.setEstado(EstadoPqr.EN_TRAMITE);
        p.setFechaCierre(null);
        p.setCalificacion(null);
        Contexto c = Contexto.get();
        c.usuarios.registrarBitacora(u.getId(), "REABRIR_PQR", p.getRadicado());
        if (p.getIdResponsable() != null) {
            c.usuarios.notificar(p.getIdResponsable(), "El usuario reabrió la PQR " + p.getRadicado(), "/api/pqr/ver?id=" + p.getId());
        }
        c.usuarios.notificarAdministradores("PQR reabierta: " + p.getRadicado(), "/api/pqr/ver?id=" + p.getId());
    }

    // ------------------------------------------------------------ consultas

    public PQR ver(Usuario u, long id) {
        PQR p = requerir(id);
        if (!puedeVer(u, p)) {
            throw new NegocioException("No tienes acceso a esta PQR.");
        }
        return p;
    }

    public boolean puedeVer(Usuario u, PQR p) {
        return u.getRol() == Rol.ADMINISTRADOR || p.getIdUsuario() == u.getId()
                || (u.getRol() == Rol.ASESOR && p.getIdResponsable() != null && p.getIdResponsable() == u.getId());
    }

    public List<PQR> listar(Usuario u) {
        List<PQR> lista = new ArrayList<>();
        for (PQR p : pqrs.todos()) {
            if (puedeVer(u, p)) {
                lista.add(p);
            }
        }
        java.util.Collections.reverse(lista);
        return lista;
    }

    /** PQR sin responder cuyo plazo venció o vence en pocos días (CU-05.5). */
    public List<PQR> vencidas(EstadoPqr estado, TipoPqr tipo, LocalDate desde, LocalDate hasta) {
        List<PQR> lista = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (PQR p : pqrs.todos()) {
            if (p.getEstado() == EstadoPqr.RESPONDIDA || p.getEstado() == EstadoPqr.CERRADA) {
                continue;
            }
            if (p.getFechaLimite().isAfter(hoy.plusDays(DIAS_ALERTA))) {
                continue;
            }
            LocalDate rad = p.getFechaRadicacion().toLocalDate();
            if ((estado != null && p.getEstado() != estado) || (tipo != null && p.getTipo() != tipo)
                    || (desde != null && rad.isBefore(desde)) || (hasta != null && rad.isAfter(hasta))) {
                continue;
            }
            lista.add(p);
        }
        return lista;
    }

    public boolean estaVencida(PQR p) {
        return p.getEstado() != EstadoPqr.RESPONDIDA && p.getEstado() != EstadoPqr.CERRADA && p.getFechaLimite().isBefore(LocalDate.now());
    }

    public long diasTranscurridos(PQR p) {
        return ChronoUnit.DAYS.between(p.getFechaRadicacion().toLocalDate(), LocalDate.now());
    }

    public List<PQR> todas() {
        return pqrs.todos();
    }

    public PQR requerir(long id) {
        PQR p = pqrs.buscar(id);
        if (p == null) {
            throw new NegocioException("La PQR no existe.");
        }
        return p;
    }

    private PQR propia(Usuario u, long id) {
        PQR p = requerir(id);
        if (p.getIdUsuario() != u.getId()) {
            throw new NegocioException("Solo quien radicó la PQR puede hacerlo.");
        }
        return p;
    }

    private void exigirAdmin(Usuario u) {
        if (u.getRol() != Rol.ADMINISTRADOR) {
            throw new NegocioException("Solo el administrador puede hacerlo.");
        }
    }
}
