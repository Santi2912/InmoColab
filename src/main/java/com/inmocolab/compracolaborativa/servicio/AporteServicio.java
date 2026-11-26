package com.inmocolab.compracolaborativa.servicio;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Secuencia;
import com.inmocolab.comun.Util;
import com.inmocolab.compracolaborativa.modelo.Aporte;
import com.inmocolab.compracolaborativa.modelo.EstadoAporte;
import com.inmocolab.compracolaborativa.modelo.EstadoGrupo;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.compracolaborativa.modelo.Participacion;
import com.inmocolab.compracolaborativa.persistencia.AporteRepositorio;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Registro y verificación de aportes (CU-03.4 y CU-08.3). */
public class AporteServicio {

    private final AporteRepositorio aportes;

    public AporteServicio(AporteRepositorio aportes) {
        this.aportes = aportes;
    }

    public Aporte registrar(Usuario u, long idGrupo, BigDecimal monto, String comprobante) {
        Contexto c = Contexto.get();
        GrupoCompra g = c.grupos.requerir(idGrupo);
        Participacion p = c.grupos.participacionActiva(g, u.getId());
        if (p == null) {
            throw new NegocioException("Debes pertenecer al grupo para registrar un aporte.");
        }
        if (g.getFechaLimite().isBefore(LocalDate.now()) || g.getEstado() == EstadoGrupo.CERRADO) {
            throw new NegocioException("La fecha límite del grupo ya pasó: el grupo está cerrado.");
        }
        if (g.getEstado() != EstadoGrupo.ABIERTO) {
            throw new NegocioException("El grupo ya no recibe aportes (estado " + Util.etiqueta(g.getEstado()) + ").");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new NegocioException("El monto debe ser mayor que cero.");
        }
        if (Util.vacio(comprobante)) {
            throw new NegocioException("Adjunta o indica el comprobante del aporte.");
        }
        if (comprobante.trim().length() > 255) {
            throw new NegocioException("El comprobante admite máximo 255 caracteres.");
        }
        BigDecimal saldo = c.grupos.saldoPendiente(g);
        if (monto.compareTo(saldo) > 0) {
            throw new NegocioException("El monto supera el saldo pendiente del objetivo. Máximo permitido: " + Util.dinero(saldo) + ".");
        }
        Aporte a = new Aporte();
        a.setIdGrupo(g.getId());
        a.setIdParticipacion(p.getId());
        a.setMonto(monto);
        a.setFecha(LocalDateTime.now());
        a.setComprobante(comprobante.trim());
        a.setEstado(EstadoAporte.PENDIENTE);
        aportes.guardar(a);
        p.getAportes().add(a);
        c.usuarios.registrarBitacora(u.getId(), "REGISTRAR_APORTE", "Aporte #" + a.getId() + " de " + Util.dinero(monto) + " al grupo #" + g.getId());
        c.usuarios.notificarAdministradores("Aporte por verificar de " + u.getNombre() + " (" + Util.dinero(monto) + ")", "/api/aportes/pendientes");
        return a;
    }

    public void verificar(Usuario admin, long idAporte, boolean confirmar, String motivo) {
        if (admin.getRol() != Rol.ADMINISTRADOR) {
            throw new NegocioException("Solo el administrador verifica aportes.");
        }
        Aporte a = aportes.buscar(idAporte);
        if (a == null || a.getEstado() != EstadoAporte.PENDIENTE) {
            throw new NegocioException("El aporte no está pendiente de verificación.");
        }
        if (!confirmar && Util.vacio(motivo)) {
            throw new NegocioException("Debes indicar el motivo del rechazo.");
        }
        Contexto c = Contexto.get();
        GrupoCompra g = c.grupos.requerir(a.getIdGrupo());
        Participacion p = participacion(g, a.getIdParticipacion());
        a.setIdVerificador(admin.getId());
        a.setEstado(confirmar ? EstadoAporte.CONFIRMADO : EstadoAporte.RECHAZADO);
        a.setMotivoRechazo(confirmar ? null : motivo.trim());
        c.grupos.recalcular(g);
        c.usuarios.registrarBitacora(admin.getId(), confirmar ? "CONFIRMAR_APORTE" : "RECHAZAR_APORTE", "Aporte #" + a.getId());
        c.usuarios.notificar(p.getIdComprador(), "Tu aporte de " + Util.dinero(a.getMonto()) + " fue "
                + (confirmar ? "confirmado." : "rechazado. Motivo: " + motivo.trim()), "/api/grupos/ver?id=" + g.getId());
    }

    public List<Aporte> pendientes() {
        return aportes.porEstado(EstadoAporte.PENDIENTE);
    }

    /** Aportes de un comprador en todos sus grupos. */
    public List<Aporte> deComprador(long idComprador) {
        List<Aporte> resultado = new ArrayList<>();
        for (GrupoCompra g : Contexto.get().grupos.todos()) {
            for (Participacion p : g.getParticipaciones()) {
                if (p.getIdComprador() == idComprador) {
                    resultado.addAll(p.getAportes());
                }
            }
        }
        return resultado;
    }

    public List<Aporte> todos() {
        return aportes.todos();
    }

    public Aporte buscar(long id) {
        return aportes.buscar(id);
    }

    public Participacion participacion(GrupoCompra g, long idParticipacion) {
        for (Participacion p : g.getParticipaciones()) {
            if (p.getId() == idParticipacion) {
                return p;
            }
        }
        throw new NegocioException("La participación no existe.");
    }

    /** Asigna un identificador a un aporte creado fuera del flujo normal (datos de ejemplo). */
    public Aporte sembrar(GrupoCompra g, Participacion p, BigDecimal monto, EstadoAporte estado) {
        Aporte a = new Aporte();
        a.setIdGrupo(g.getId());
        a.setIdParticipacion(p.getId());
        a.setMonto(monto);
        a.setFecha(LocalDateTime.now().minusDays(2));
        a.setComprobante("comprobante-" + Secuencia.siguiente() + ".pdf");
        a.setEstado(estado);
        aportes.guardar(a);
        p.getAportes().add(a);
        return a;
    }
}
