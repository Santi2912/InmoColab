package com.inmocolab.compracolaborativa.servicio;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Secuencia;
import com.inmocolab.comun.Util;
import com.inmocolab.compracolaborativa.modelo.Aporte;
import com.inmocolab.compracolaborativa.modelo.EstadoAporte;
import com.inmocolab.compracolaborativa.modelo.EstadoGrupo;
import com.inmocolab.compracolaborativa.modelo.EstadoInvitacion;
import com.inmocolab.compracolaborativa.modelo.EstadoParticipacion;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.compracolaborativa.modelo.Invitacion;
import com.inmocolab.compracolaborativa.modelo.Participacion;
import com.inmocolab.compracolaborativa.persistencia.GrupoRepositorio;
import com.inmocolab.inmuebles.modelo.EstadoInmueble;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Grupos de compra colaborativa y cálculo de participación (CU-03). */
public class GrupoServicio {

    private final GrupoRepositorio grupos;

    public GrupoServicio(GrupoRepositorio grupos) {
        this.grupos = grupos;
    }

    // ------------------------------------------------------------ CU-03.1 Crear

    public GrupoCompra crear(Usuario u, long idInmueble, BigDecimal objetivo, LocalDate fechaLimite) {
        exigirComprador(u);
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(idInmueble);
        if (!c.inmuebles.estaPublicado(i) || i.getEstado() != EstadoInmueble.DISPONIBLE) {
            throw new NegocioException("Solo se pueden crear grupos sobre inmuebles aprobados y disponibles.");
        }
        if (objetivo == null || objetivo.signum() <= 0) {
            throw new NegocioException("El monto objetivo debe ser mayor que cero.");
        }
        if (objetivo.compareTo(i.getPrecio()) > 0) {
            throw new NegocioException("El monto objetivo no puede superar el precio del inmueble (" + Util.dinero(i.getPrecio()) + ").");
        }
        if (fechaLimite == null || !fechaLimite.isAfter(LocalDate.now())) {
            throw new NegocioException("La fecha límite debe ser posterior a hoy.");
        }
        int vigencia = c.usuarios.enteroParametro("vigencia_grupo_dias", 90);
        if (fechaLimite.isAfter(LocalDate.now().plusDays(vigencia))) {
            throw new NegocioException("La fecha límite no puede superar " + vigencia + " días desde hoy.");
        }
        GrupoCompra g = new GrupoCompra();
        g.setIdInmueble(idInmueble);
        g.setIdCreador(u.getId());
        g.setMontoObjetivo(objetivo);
        g.setMontoRecaudado(BigDecimal.ZERO);
        g.setFechaCreacion(LocalDateTime.now());
        g.setFechaLimite(fechaLimite);
        g.setEstado(EstadoGrupo.ABIERTO);
        grupos.guardar(g);
        agregarIntegrante(g, u.getId());
        c.usuarios.registrarBitacora(u.getId(), "CREAR_GRUPO", "Grupo #" + g.getId() + " para inmueble #" + idInmueble);
        return g;
    }

    // ------------------------------------------------------------ CU-03.2 Invitar / CU-03.3 Unirse

    public void invitar(Usuario u, long idGrupo, String correo) {
        GrupoCompra g = requerir(idGrupo);
        exigirIntegrante(u, g);
        if (g.getEstado() != EstadoGrupo.ABIERTO) {
            throw new NegocioException("Solo se invita a grupos abiertos.");
        }
        Contexto c = Contexto.get();
        Usuario invitado = Util.vacio(correo) ? null : c.usuarios.porCorreo(correo.trim());
        if (invitado == null || invitado.getRol() != Rol.COMPRADOR || invitado.getEstado() != EstadoUsuario.ACTIVO) {
            throw new NegocioException("Solo se puede invitar a compradores registrados y activos, por su correo.");
        }
        if (participacionActiva(g, invitado.getId()) != null) {
            throw new NegocioException("Ese comprador ya pertenece al grupo.");
        }
        Invitacion inv = new Invitacion();
        inv.setId(Secuencia.siguiente());
        inv.setIdGrupo(g.getId());
        inv.setIdInvitado(invitado.getId());
        inv.setFechaEnvio(LocalDateTime.now());
        inv.setEstado(EstadoInvitacion.PENDIENTE);
        g.getInvitaciones().add(inv);
        c.usuarios.notificar(invitado.getId(), u.getNombre() + " te invitó a su grupo de compra #" + g.getId()
                + ". La invitación vence en 7 días.", "/api/grupos/ver?id=" + g.getId());
    }

    public void unirse(Usuario u, long idGrupo) {
        exigirComprador(u);
        GrupoCompra g = requerir(idGrupo);
        if (g.getEstado() != EstadoGrupo.ABIERTO) {
            throw new NegocioException("El grupo no está abierto.");
        }
        if (participacionActiva(g, u.getId()) != null) {
            throw new NegocioException("Ya perteneces a este grupo.");
        }
        for (Invitacion inv : g.getInvitaciones()) {
            if (inv.getIdInvitado() == u.getId() && inv.getEstado() == EstadoInvitacion.PENDIENTE) {
                inv.setEstado(inv.getFechaEnvio().plusDays(7).isBefore(LocalDateTime.now())
                        ? EstadoInvitacion.VENCIDA : EstadoInvitacion.ACEPTADA);
            }
        }
        agregarIntegrante(g, u.getId());
        Contexto.get().usuarios.notificar(g.getIdCreador(), u.getNombre() + " se unió a tu grupo #" + g.getId(),
                "/api/grupos/ver?id=" + g.getId());
    }

    // ------------------------------------------------------------ CU-03.6 Abandonar

    public void abandonar(Usuario u, long idGrupo) {
        GrupoCompra g = requerir(idGrupo);
        Participacion p = participacionActiva(g, u.getId());
        if (p == null) {
            throw new NegocioException("No perteneces a este grupo.");
        }
        if (g.getEstado() == EstadoGrupo.EN_OFERTA || g.getEstado() == EstadoGrupo.CERRADO) {
            throw new NegocioException("Solo puedes abandonar el grupo antes de que presente una oferta.");
        }
        p.setEstado(EstadoParticipacion.RETIRADA);
        for (Aporte a : p.getAportes()) {
            if (a.getEstado() == EstadoAporte.PENDIENTE || a.getEstado() == EstadoAporte.CONFIRMADO) {
                a.setEstado(EstadoAporte.EN_DEVOLUCION);
            }
        }
        recalcular(g);
        Contexto c = Contexto.get();
        c.usuarios.registrarBitacora(u.getId(), "ABANDONAR_GRUPO", "Grupo #" + g.getId());
        for (Participacion otro : g.getParticipaciones()) {
            if (otro.getEstado() == EstadoParticipacion.ACTIVA) {
                c.usuarios.notificar(otro.getIdComprador(), u.getNombre() + " abandonó el grupo #" + g.getId()
                        + ". Se recalculó la participación.", "/api/grupos/ver?id=" + g.getId());
            }
        }
        if (activos(g) == 0) {
            g.setEstado(EstadoGrupo.CERRADO);
        }
    }

    // ------------------------------------------------------------ CU-03.5 Participación

    /** Participación = aporte confirmado / total confirmado del grupo × 100. */
    public void recalcular(GrupoCompra g) {
        BigDecimal total = BigDecimal.ZERO;
        for (Participacion p : g.getParticipaciones()) {
            if (p.getEstado() == EstadoParticipacion.ACTIVA) {
                total = total.add(confirmado(p));
            }
        }
        for (Participacion p : g.getParticipaciones()) {
            if (p.getEstado() == EstadoParticipacion.ACTIVA && total.signum() > 0) {
                p.setPorcentaje(confirmado(p).multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP));
            } else {
                p.setPorcentaje(BigDecimal.ZERO);
            }
        }
        g.setMontoRecaudado(total);
        if (g.getEstado() == EstadoGrupo.ABIERTO && total.compareTo(g.getMontoObjetivo()) >= 0) {
            g.setEstado(EstadoGrupo.COMPLETO);
            Contexto.get().usuarios.notificar(g.getIdCreador(), "¡Tu grupo #" + g.getId() + " alcanzó el monto objetivo! Ya pueden hacer una oferta.",
                    "/api/grupos/ver?id=" + g.getId());
        } else if (g.getEstado() == EstadoGrupo.COMPLETO && total.compareTo(g.getMontoObjetivo()) < 0) {
            g.setEstado(EstadoGrupo.ABIERTO);
        }
    }

    public BigDecimal confirmado(Participacion p) {
        BigDecimal suma = BigDecimal.ZERO;
        for (Aporte a : p.getAportes()) {
            if (a.getEstado() == EstadoAporte.CONFIRMADO) {
                suma = suma.add(a.getMonto());
            }
        }
        return suma;
    }

    /** Objetivo menos los aportes comprometidos (pendientes y confirmados) de integrantes activos. */
    public BigDecimal saldoPendiente(GrupoCompra g) {
        BigDecimal comprometido = BigDecimal.ZERO;
        for (Participacion p : g.getParticipaciones()) {
            if (p.getEstado() != EstadoParticipacion.ACTIVA) {
                continue;
            }
            for (Aporte a : p.getAportes()) {
                if (a.getEstado() == EstadoAporte.PENDIENTE || a.getEstado() == EstadoAporte.CONFIRMADO) {
                    comprometido = comprometido.add(a.getMonto());
                }
            }
        }
        return g.getMontoObjetivo().subtract(comprometido).max(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------ cambios de estado usados por Negociación

    public void marcarEnOferta(GrupoCompra g) {
        g.setEstado(EstadoGrupo.EN_OFERTA);
    }

    public void liberarOferta(GrupoCompra g) {
        if (g.getEstado() == EstadoGrupo.EN_OFERTA) {
            g.setEstado(EstadoGrupo.COMPLETO);
        }
    }

    public void cerrar(GrupoCompra g) {
        g.setEstado(EstadoGrupo.CERRADO);
    }

    // ------------------------------------------------------------ consultas

    public GrupoCompra requerir(long id) {
        GrupoCompra g = grupos.buscar(id);
        if (g == null) {
            throw new NegocioException("El grupo no existe.");
        }
        if (g.getEstado() == EstadoGrupo.ABIERTO && g.getFechaLimite().isBefore(LocalDate.now())) {
            g.setEstado(EstadoGrupo.CERRADO);
        }
        return g;
    }

    public List<GrupoCompra> abiertosPorInmueble(long idInmueble) {
        return grupos.abiertosPorInmueble(idInmueble);
    }

    public List<GrupoCompra> porInmueble(long idInmueble) {
        return grupos.porInmueble(idInmueble);
    }

    public List<GrupoCompra> misGrupos(long idComprador) {
        return grupos.porIntegrante(idComprador);
    }

    public List<GrupoCompra> todos() {
        return grupos.todos();
    }

    public Participacion participacionActiva(GrupoCompra g, long idComprador) {
        for (Participacion p : g.getParticipaciones()) {
            if (p.getIdComprador() == idComprador && p.getEstado() == EstadoParticipacion.ACTIVA) {
                return p;
            }
        }
        return null;
    }

    public int activos(GrupoCompra g) {
        int n = 0;
        for (Participacion p : g.getParticipaciones()) {
            if (p.getEstado() == EstadoParticipacion.ACTIVA) {
                n++;
            }
        }
        return n;
    }

    private void agregarIntegrante(GrupoCompra g, long idComprador) {
        Participacion previa = null;
        for (Participacion p : g.getParticipaciones()) {
            if (p.getIdComprador() == idComprador) {
                previa = p;
            }
        }
        if (previa != null) {
            previa.setEstado(EstadoParticipacion.ACTIVA);
            previa.setFechaIngreso(LocalDateTime.now());
        } else {
            Participacion p = new Participacion();
            p.setId(Secuencia.siguiente());
            p.setIdGrupo(g.getId());
            p.setIdComprador(idComprador);
            p.setPorcentaje(BigDecimal.ZERO);
            p.setFechaIngreso(LocalDateTime.now());
            p.setEstado(EstadoParticipacion.ACTIVA);
            g.getParticipaciones().add(p);
        }
    }

    private void exigirComprador(Usuario u) {
        if (u.getRol() != Rol.COMPRADOR) {
            throw new NegocioException("Solo los compradores participan en grupos de compra.");
        }
    }

    private void exigirIntegrante(Usuario u, GrupoCompra g) {
        if (participacionActiva(g, u.getId()) == null) {
            throw new NegocioException("Debes ser integrante del grupo.");
        }
    }
}
