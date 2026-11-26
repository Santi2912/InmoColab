package com.inmocolab.negociacion.servicio;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Secuencia;
import com.inmocolab.comun.Util;
import com.inmocolab.compracolaborativa.modelo.EstadoParticipacion;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.compracolaborativa.modelo.Participacion;
import com.inmocolab.inmuebles.modelo.EstadoInmueble;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.negociacion.modelo.CambioEstado;
import com.inmocolab.negociacion.modelo.EstadoNegociacion;
import com.inmocolab.negociacion.modelo.EstadoOferta;
import com.inmocolab.negociacion.modelo.Interes;
import com.inmocolab.negociacion.modelo.Negociacion;
import com.inmocolab.negociacion.modelo.Observacion;
import com.inmocolab.negociacion.modelo.Oferta;
import com.inmocolab.negociacion.persistencia.InteresRepositorio;
import com.inmocolab.negociacion.persistencia.NegociacionRepositorio;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Interés, estado, seguimiento y cierre de negociaciones (CU-04). */
public class NegociacionServicio {

    private final NegociacionRepositorio negociaciones;
    private final InteresRepositorio intereses;

    public NegociacionServicio(NegociacionRepositorio negociaciones, InteresRepositorio intereses) {
        this.negociaciones = negociaciones;
        this.intereses = intereses;
    }

    // ------------------------------------------------------------ CU-04.1 Manifestar interés

    public void manifestarInteres(Usuario u, long idInmueble) {
        if (u.getRol() != Rol.COMPRADOR) {
            throw new NegocioException("Solo los compradores pueden manifestar interés.");
        }
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(idInmueble);
        if (!c.inmuebles.estaPublicado(i)) {
            throw new NegocioException("El inmueble no está disponible.");
        }
        if (intereses.existe(idInmueble, u.getId())) {
            throw new NegocioException("Ya manifestaste interés por este inmueble.");
        }
        Interes in = new Interes();
        in.setIdInmueble(idInmueble);
        in.setIdComprador(u.getId());
        in.setFecha(LocalDateTime.now());
        intereses.guardar(in);
        vigenteOCrear(i);
        c.usuarios.notificar(i.getIdPropietario(), u.getNombre() + " manifestó interés en " + i.getDireccion(),
                "/api/inmuebles/detalle?id=" + i.getId());
        c.usuarios.registrarBitacora(u.getId(), "MANIFESTAR_INTERES", "Inmueble #" + i.getId());
    }

    public List<Interes> interesesDe(long idInmueble) {
        return intereses.porInmueble(idInmueble);
    }

    public List<Negociacion> negociacionesDe(long idInmueble) {
        return negociaciones.porInmueble(idInmueble);
    }

    public boolean tieneInteres(long idInmueble, long idComprador) {
        return intereses.existe(idInmueble, idComprador);
    }

    public int cantidadInteresados(long idInmueble) {
        return interesados(idInmueble).size();
    }

    /** Compradores interesados o que han ofertado por el inmueble. */
    public List<Long> interesados(long idInmueble) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Interes in : intereses.porInmueble(idInmueble)) {
            ids.add(in.getIdComprador());
        }
        for (Negociacion n : negociaciones.porInmueble(idInmueble)) {
            ids.addAll(compradores(n));
        }
        return new ArrayList<>(ids);
    }

    // ------------------------------------------------------------ negociación vigente

    public Negociacion vigenteOCrear(Inmueble i) {
        synchronized (this) {
            Negociacion n = negociaciones.activaPorInmueble(i.getId());
            if (n == null) {
                n = new Negociacion();
                n.setIdInmueble(i.getId());
                n.setIdAsesor(i.getIdAsesor());
                n.setFechaInicio(LocalDateTime.now());
                n.setUltimaActividad(LocalDateTime.now());
                n.setEstado(EstadoNegociacion.INICIADA);
                n.getHistorial().add(new CambioEstado(EstadoNegociacion.INICIADA, LocalDateTime.now(), "Se inició la negociación"));
                negociaciones.guardar(n);
            }
            return n;
        }
    }

    public void cambiarEstado(Negociacion n, EstadoNegociacion estado, String detalle) {
        n.setEstado(estado);
        n.setUltimaActividad(LocalDateTime.now());
        n.getHistorial().add(new CambioEstado(estado, LocalDateTime.now(), detalle));
    }

    public boolean tieneOfertaAceptada(long idInmueble) {
        Negociacion n = negociaciones.activaPorInmueble(idInmueble);
        return n != null && (n.getEstado() == EstadoNegociacion.ACEPTADA || n.getEstado() == EstadoNegociacion.EN_FORMALIZACION);
    }

    public Negociacion vigente(long idInmueble) {
        return negociaciones.activaPorInmueble(idInmueble);
    }

    // ------------------------------------------------------------ CU-04.3 Consultar

    public Negociacion ver(Usuario u, long id) {
        Negociacion n = requerir(id);
        if (!puedeVer(u, n)) {
            throw new NegocioException("No tienes acceso a esta negociación.");
        }
        return n;
    }

    public Negociacion requerir(long id) {
        Negociacion n = negociaciones.buscar(id);
        if (n == null) {
            throw new NegocioException("La negociación no existe.");
        }
        return n;
    }

    public boolean puedeVer(Usuario u, Negociacion n) {
        if (u.getRol() == Rol.ADMINISTRADOR) {
            return true;
        }
        Inmueble i = Contexto.get().inmuebles.requerir(n.getIdInmueble());
        if (u.getRol() == Rol.PROPIETARIO) {
            return i.getIdPropietario() == u.getId();
        }
        if (u.getRol() == Rol.ASESOR) {
            return n.getIdAsesor() != null && n.getIdAsesor() == u.getId();
        }
        return intereses.existe(n.getIdInmueble(), u.getId()) || compradores(n).contains(u.getId());
    }

    public List<Negociacion> misNegociaciones(Usuario u) {
        List<Negociacion> resultado = new ArrayList<>();
        for (Negociacion n : negociaciones.todos()) {
            if (puedeVer(u, n)) {
                resultado.add(n);
            }
        }
        return resultado;
    }

    public List<Negociacion> todas() {
        return negociaciones.todos();
    }

    /** Compradores que ofertaron (individual o por su grupo) en la negociación. */
    public Set<Long> compradores(Negociacion n) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Oferta o : n.getOfertas()) {
            if (o.getIdComprador() != null) {
                ids.add(o.getIdComprador());
            }
            if (o.getIdGrupo() != null) {
                GrupoCompra g = Contexto.get().grupos.requerir(o.getIdGrupo());
                for (Participacion p : g.getParticipaciones()) {
                    if (p.getEstado() == EstadoParticipacion.ACTIVA) {
                        ids.add(p.getIdComprador());
                    }
                }
            }
        }
        return ids;
    }

    // ------------------------------------------------------------ CU-04.6 Acompañar

    public void observar(Usuario asesor, long idNegociacion, String texto) {
        Negociacion n = requerir(idNegociacion);
        if (asesor.getRol() != Rol.ASESOR || n.getIdAsesor() == null || n.getIdAsesor() != asesor.getId()) {
            throw new NegocioException("Solo el asesor asignado puede registrar observaciones.");
        }
        if (Util.vacio(texto)) {
            throw new NegocioException("Escribe la observación.");
        }
        Observacion o = new Observacion();
        o.setId(Secuencia.siguiente());
        o.setIdAsesor(asesor.getId());
        o.setTexto(texto.trim());
        o.setFecha(LocalDateTime.now());
        n.getObservaciones().add(o);
        n.setUltimaActividad(LocalDateTime.now());
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(n.getIdInmueble());
        String enlace = "/api/negociaciones/ver?id=" + n.getId();
        c.usuarios.notificar(i.getIdPropietario(), "El asesor dejó una observación en la negociación #" + n.getId(), enlace);
        for (Long idComprador : compradores(n)) {
            c.usuarios.notificar(idComprador, "El asesor dejó una observación en la negociación #" + n.getId(), enlace);
        }
    }

    public void asignarAsesor(Usuario admin, long idNegociacion, long idAsesor) {
        Negociacion n = requerir(idNegociacion);
        Contexto c = Contexto.get();
        Usuario asesor = c.usuarios.buscar(idAsesor);
        if (asesor == null || asesor.getRol() != Rol.ASESOR || asesor.getEstado() != EstadoUsuario.ACTIVO) {
            throw new NegocioException("Solo se pueden asignar asesores activos.");
        }
        n.setIdAsesor(asesor.getId());
        c.usuarios.registrarBitacora(admin.getId(), "ASIGNAR_ASESOR", "Negociación #" + n.getId() + " -> " + asesor.getNombre());
        c.usuarios.notificar(asesor.getId(), "Se te asignó la negociación #" + n.getId(), "/api/negociaciones/ver?id=" + n.getId());
    }

    // ------------------------------------------------------------ cierre y cancelación

    public void cerrar(Usuario u, long idNegociacion) {
        Negociacion n = requerir(idNegociacion);
        Inmueble i = Contexto.get().inmuebles.requerir(n.getIdInmueble());
        exigirPropietarioOAdmin(u, i);
        if (n.getEstado() != EstadoNegociacion.EN_FORMALIZACION) {
            throw new NegocioException("Solo se puede cerrar una negociación en formalización.");
        }
        cambiarEstado(n, EstadoNegociacion.CERRADA, "Venta formalizada");
        n.setFechaFin(LocalDateTime.now());
        i.setEstado(EstadoInmueble.VENDIDO);
        Contexto c = Contexto.get();
        for (Oferta o : n.getOfertas()) {
            if (o.getEstado() == EstadoOferta.ACEPTADA && o.getIdGrupo() != null) {
                c.grupos.cerrar(c.grupos.requerir(o.getIdGrupo()));
            }
        }
        c.usuarios.registrarBitacora(u.getId(), "CERRAR_NEGOCIACION", "Negociación #" + n.getId());
        notificarTodos(n, "La negociación #" + n.getId() + " se cerró: venta formalizada. Ya puedes calificar la experiencia.");
    }

    public void cancelar(Usuario u, long idNegociacion, String motivo) {
        Negociacion n = requerir(idNegociacion);
        Inmueble i = Contexto.get().inmuebles.requerir(n.getIdInmueble());
        exigirPropietarioOAdmin(u, i);
        if (n.getEstado() == EstadoNegociacion.CERRADA || n.getEstado() == EstadoNegociacion.CANCELADA) {
            throw new NegocioException("La negociación ya terminó.");
        }
        if (Util.vacio(motivo)) {
            throw new NegocioException("Indica el motivo de la cancelación.");
        }
        terminarCancelada(n, i, motivo.trim());
        Contexto.get().usuarios.registrarBitacora(u.getId(), "CANCELAR_NEGOCIACION", "Negociación #" + n.getId() + ". " + motivo.trim());
    }

    /** Cancela la negociación vigente cuando el propietario retira el inmueble. */
    public void cancelarPorRetiro(long idInmueble) {
        Negociacion n = negociaciones.activaPorInmueble(idInmueble);
        if (n != null) {
            terminarCancelada(n, Contexto.get().inmuebles.requerir(idInmueble), "El propietario retiró la publicación");
        }
    }

    private void terminarCancelada(Negociacion n, Inmueble i, String motivo) {
        Contexto c = Contexto.get();
        for (Oferta o : n.getOfertas()) {
            if (o.getEstado() == EstadoOferta.PENDIENTE || o.getEstado() == EstadoOferta.ACEPTADA) {
                o.setEstado(EstadoOferta.RECHAZADA);
                o.setMotivoRechazo("Negociación cancelada: " + motivo);
                if (o.getIdGrupo() != null) {
                    c.grupos.liberarOferta(c.grupos.requerir(o.getIdGrupo()));
                }
            }
        }
        cambiarEstado(n, EstadoNegociacion.CANCELADA, motivo);
        n.setFechaFin(LocalDateTime.now());
        if (i.getEstado() == EstadoInmueble.EN_NEGOCIACION) {
            i.setEstado(EstadoInmueble.DISPONIBLE);
        }
        notificarTodos(n, "La negociación #" + n.getId() + " fue cancelada. Motivo: " + motivo);
    }

    public void notificarTodos(Negociacion n, String mensaje) {
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(n.getIdInmueble());
        String enlace = "/api/negociaciones/ver?id=" + n.getId();
        c.usuarios.notificar(i.getIdPropietario(), mensaje, enlace);
        for (Long idComprador : compradores(n)) {
            c.usuarios.notificar(idComprador, mensaje, enlace);
        }
        if (n.getIdAsesor() != null) {
            c.usuarios.notificar(n.getIdAsesor(), mensaje, enlace);
        }
    }

    private void exigirPropietarioOAdmin(Usuario u, Inmueble i) {
        if (u.getRol() != Rol.ADMINISTRADOR && !(u.getRol() == Rol.PROPIETARIO && i.getIdPropietario() == u.getId())) {
            throw new NegocioException("Solo el propietario del inmueble o el administrador pueden hacer esto.");
        }
    }
}
