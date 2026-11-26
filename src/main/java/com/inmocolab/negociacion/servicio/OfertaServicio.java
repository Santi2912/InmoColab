package com.inmocolab.negociacion.servicio;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Util;
import com.inmocolab.compracolaborativa.modelo.EstadoGrupo;
import com.inmocolab.compracolaborativa.modelo.EstadoParticipacion;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.compracolaborativa.modelo.Participacion;
import com.inmocolab.inmuebles.modelo.EstadoInmueble;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.negociacion.modelo.EstadoNegociacion;
import com.inmocolab.negociacion.modelo.EstadoOferta;
import com.inmocolab.negociacion.modelo.Negociacion;
import com.inmocolab.negociacion.modelo.Oferta;
import com.inmocolab.negociacion.persistencia.OfertaRepositorio;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ofertas individuales o de grupo y su aceptación o rechazo (CU-04.2, 04.4 y 04.5). */
public class OfertaServicio {

    private final OfertaRepositorio ofertas;

    public OfertaServicio(OfertaRepositorio ofertas) {
        this.ofertas = ofertas;
    }

    public Oferta ofertar(Usuario u, long idInmueble, BigDecimal monto, Long idGrupo, String condiciones) {
        if (u.getRol() != Rol.COMPRADOR) {
            throw new NegocioException("Solo los compradores pueden hacer ofertas.");
        }
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(idInmueble);
        if (!c.inmuebles.estaPublicado(i)) {
            throw new NegocioException("El inmueble no está publicado o disponible.");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new NegocioException("El monto de la oferta debe ser mayor que cero.");
        }
        if (monto.compareTo(i.getPrecio()) > 0) {
            throw new NegocioException("La oferta no puede superar el precio publicado (" + Util.dinero(i.getPrecio()) + ").");
        }
        if (monto.compareTo(i.getPrecioMinimo()) < 0) {
            throw new NegocioException("El monto es menor al mínimo permitido por el propietario: " + Util.dinero(i.getPrecioMinimo()) + ".");
        }
        Negociacion n = c.negociaciones.vigenteOCrear(i);
        if (n.getEstado() == EstadoNegociacion.ACEPTADA || n.getEstado() == EstadoNegociacion.EN_FORMALIZACION) {
            throw new NegocioException("Este inmueble ya tiene una oferta aceptada.");
        }
        GrupoCompra g = null;
        if (idGrupo != null) {
            g = c.grupos.requerir(idGrupo);
            Participacion p = c.grupos.participacionActiva(g, u.getId());
            if (p == null || g.getIdInmueble() != idInmueble) {
                throw new NegocioException("Solo puedes ofertar con un grupo al que perteneces y que sea de este inmueble.");
            }
            if (g.getEstado() != EstadoGrupo.COMPLETO) {
                throw new NegocioException("El grupo debe haber alcanzado el monto objetivo para ofertar (estado actual: "
                        + Util.etiqueta(g.getEstado()) + ").");
            }
            if (g.getMontoRecaudado().compareTo(monto) < 0) {
                throw new NegocioException("El monto confirmado del grupo (" + Util.dinero(g.getMontoRecaudado())
                        + ") no cubre el valor ofertado.");
            }
        }
        for (Oferta o : n.getOfertas()) {
            if (o.getEstado() != EstadoOferta.PENDIENTE) {
                continue;
            }
            boolean mismoGrupo = idGrupo != null && idGrupo.equals(o.getIdGrupo());
            boolean mismoComprador = idGrupo == null && o.getIdGrupo() == null && o.getIdComprador() != null && o.getIdComprador() == u.getId();
            if (mismoGrupo || mismoComprador) {
                throw new NegocioException("Ya tienes una oferta activa: retírala antes de hacer otra.");
            }
        }
        Oferta o = new Oferta();
        o.setIdNegociacion(n.getId());
        o.setIdComprador(idGrupo == null ? u.getId() : null);
        o.setIdGrupo(idGrupo);
        o.setMonto(monto);
        o.setCondiciones(Util.vacio(condiciones) ? null : condiciones.trim());
        o.setFecha(LocalDateTime.now());
        o.setEstado(EstadoOferta.PENDIENTE);
        ofertas.guardar(o);
        n.getOfertas().add(o);
        c.negociaciones.cambiarEstado(n, EstadoNegociacion.EN_OFERTA, "Nueva oferta de " + Util.dinero(monto) + " por " + u.getNombre());
        i.setEstado(EstadoInmueble.EN_NEGOCIACION);
        if (g != null) {
            c.grupos.marcarEnOferta(g);
        }
        String enlace = "/api/negociaciones/ver?id=" + n.getId();
        c.usuarios.notificar(i.getIdPropietario(), "Recibiste una oferta de " + Util.dinero(monto) + " por " + i.getDireccion(), enlace);
        if (n.getIdAsesor() != null) {
            c.usuarios.notificar(n.getIdAsesor(), "Nueva oferta en la negociación #" + n.getId(), enlace);
        }
        c.usuarios.registrarBitacora(u.getId(), "REALIZAR_OFERTA", "Oferta #" + o.getId() + " por " + Util.dinero(monto));
        return o;
    }

    public void retirar(Usuario u, long idOferta) {
        Oferta o = requerir(idOferta);
        boolean propia = o.getIdComprador() != null && o.getIdComprador() == u.getId();
        boolean delGrupo = o.getIdGrupo() != null
                && Contexto.get().grupos.participacionActiva(Contexto.get().grupos.requerir(o.getIdGrupo()), u.getId()) != null;
        if (!propia && !delGrupo) {
            throw new NegocioException("Solo quien hizo la oferta puede retirarla.");
        }
        if (o.getEstado() != EstadoOferta.PENDIENTE) {
            throw new NegocioException("Solo se pueden retirar ofertas pendientes.");
        }
        liberar(o, EstadoOferta.RETIRADA, "Retirada por el comprador");
    }

    public void aceptar(Usuario owner, long idOferta) {
        Oferta o = requerir(idOferta);
        Contexto c = Contexto.get();
        Negociacion n = c.negociaciones.requerir(o.getIdNegociacion());
        Inmueble i = c.inmuebles.requerir(n.getIdInmueble());
        exigirPropietario(owner, i);
        if (o.getEstado() != EstadoOferta.PENDIENTE) {
            throw new NegocioException("La oferta ya no está pendiente.");
        }
        o.setEstado(EstadoOferta.ACEPTADA);
        for (Oferta otra : n.getOfertas()) {
            if (otra.getId() != o.getId() && otra.getEstado() == EstadoOferta.PENDIENTE) {
                liberar(otra, EstadoOferta.RECHAZADA, "Se aceptó otra oferta");
            }
        }
        c.negociaciones.cambiarEstado(n, EstadoNegociacion.ACEPTADA, "Oferta de " + Util.dinero(o.getMonto()) + " aceptada");
        c.negociaciones.cambiarEstado(n, EstadoNegociacion.EN_FORMALIZACION, "Inicia la formalización");
        c.usuarios.registrarBitacora(owner.getId(), "ACEPTAR_OFERTA", "Oferta #" + o.getId());
        c.negociaciones.notificarTodos(n, "Se aceptó la oferta de " + Util.dinero(o.getMonto()) + " por " + i.getDireccion()
                + ". La negociación #" + n.getId() + " pasa a formalización.");
    }

    public void rechazar(Usuario owner, long idOferta, String motivo) {
        Oferta o = requerir(idOferta);
        Contexto c = Contexto.get();
        Negociacion n = c.negociaciones.requerir(o.getIdNegociacion());
        Inmueble i = c.inmuebles.requerir(n.getIdInmueble());
        exigirPropietario(owner, i);
        if (o.getEstado() != EstadoOferta.PENDIENTE) {
            throw new NegocioException("La oferta ya no está pendiente.");
        }
        if (Util.vacio(motivo)) {
            throw new NegocioException("Debes indicar el motivo del rechazo.");
        }
        liberar(o, EstadoOferta.RECHAZADA, motivo.trim());
        c.usuarios.registrarBitacora(owner.getId(), "RECHAZAR_OFERTA", "Oferta #" + o.getId() + ". " + motivo.trim());
    }

    public Oferta requerir(long id) {
        Oferta o = ofertas.buscar(id);
        if (o == null) {
            throw new NegocioException("La oferta no existe.");
        }
        return o;
    }

    public java.util.List<Oferta> todas() {
        return ofertas.todos();
    }

    /** Marca la oferta como terminada, libera su grupo y, si ya no quedan pendientes, reabre el inmueble. */
    private void liberar(Oferta o, EstadoOferta estado, String motivo) {
        Contexto c = Contexto.get();
        o.setEstado(estado);
        o.setMotivoRechazo(motivo);
        if (o.getIdGrupo() != null) {
            c.grupos.liberarOferta(c.grupos.requerir(o.getIdGrupo()));
        }
        Negociacion n = c.negociaciones.requerir(o.getIdNegociacion());
        Inmueble i = c.inmuebles.requerir(n.getIdInmueble());
        String enlace = "/api/negociaciones/ver?id=" + n.getId();
        String texto = estado == EstadoOferta.RETIRADA ? "Se retiró la oferta de " + Util.dinero(o.getMonto())
                : "Tu oferta de " + Util.dinero(o.getMonto()) + " fue rechazada. Motivo: " + motivo;
        if (estado == EstadoOferta.RETIRADA) {
            c.usuarios.notificar(i.getIdPropietario(), texto + " por " + i.getDireccion(), enlace);
        } else {
            if (o.getIdComprador() != null) {
                c.usuarios.notificar(o.getIdComprador(), texto, enlace);
            }
            if (o.getIdGrupo() != null) {
                for (Participacion p : c.grupos.requerir(o.getIdGrupo()).getParticipaciones()) {
                    if (p.getEstado() == EstadoParticipacion.ACTIVA) {
                        c.usuarios.notificar(p.getIdComprador(), texto, enlace);
                    }
                }
            }
        }
        boolean quedanPendientes = false;
        for (Oferta x : n.getOfertas()) {
            quedanPendientes |= x.getEstado() == EstadoOferta.PENDIENTE || x.getEstado() == EstadoOferta.ACEPTADA;
        }
        if (!quedanPendientes && n.getEstado() == EstadoNegociacion.EN_OFERTA) {
            c.negociaciones.cambiarEstado(n, EstadoNegociacion.INICIADA, "Sin ofertas pendientes");
            if (i.getEstado() == EstadoInmueble.EN_NEGOCIACION) {
                i.setEstado(EstadoInmueble.DISPONIBLE);
            }
        }
    }

    private void exigirPropietario(Usuario u, Inmueble i) {
        if (u.getRol() != Rol.PROPIETARIO || i.getIdPropietario() != u.getId()) {
            throw new NegocioException("Solo el propietario del inmueble puede responder la oferta.");
        }
    }
}
