package com.inmocolab.compracolaborativa.modelo;

import com.inmocolab.comun.Entidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GrupoCompra extends Entidad {

    private long idInmueble;
    private long idCreador;
    private BigDecimal montoObjetivo;
    private BigDecimal montoRecaudado;
    private LocalDateTime fechaCreacion;
    private LocalDate fechaLimite;
    private EstadoGrupo estado;
    private List<Participacion> participaciones = new ArrayList<>();
    private List<Invitacion> invitaciones = new ArrayList<>();

    public long getIdInmueble() {
        return idInmueble;
    }

    public void setIdInmueble(long idInmueble) {
        this.idInmueble = idInmueble;
    }

    public long getIdCreador() {
        return idCreador;
    }

    public void setIdCreador(long idCreador) {
        this.idCreador = idCreador;
    }

    public BigDecimal getMontoObjetivo() {
        return montoObjetivo;
    }

    public void setMontoObjetivo(BigDecimal montoObjetivo) {
        this.montoObjetivo = montoObjetivo;
    }

    public BigDecimal getMontoRecaudado() {
        return montoRecaudado;
    }

    public void setMontoRecaudado(BigDecimal montoRecaudado) {
        this.montoRecaudado = montoRecaudado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public EstadoGrupo getEstado() {
        return estado;
    }

    public void setEstado(EstadoGrupo estado) {
        this.estado = estado;
    }

    public List<Participacion> getParticipaciones() {
        return participaciones;
    }

    public void setParticipaciones(List<Participacion> participaciones) {
        this.participaciones = participaciones;
    }

    public List<Invitacion> getInvitaciones() {
        return invitaciones;
    }

    public void setInvitaciones(List<Invitacion> invitaciones) {
        this.invitaciones = invitaciones;
    }

}
