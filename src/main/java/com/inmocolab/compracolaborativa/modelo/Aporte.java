package com.inmocolab.compracolaborativa.modelo;

import com.inmocolab.comun.Entidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Aporte extends Entidad {

    private long idGrupo;
    private long idParticipacion;
    private Long idVerificador;
    private BigDecimal monto;
    private LocalDateTime fecha;
    private String comprobante;
    private EstadoAporte estado;
    private String motivoRechazo;

    public long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public long getIdParticipacion() {
        return idParticipacion;
    }

    public void setIdParticipacion(long idParticipacion) {
        this.idParticipacion = idParticipacion;
    }

    public Long getIdVerificador() {
        return idVerificador;
    }

    public void setIdVerificador(Long idVerificador) {
        this.idVerificador = idVerificador;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getComprobante() {
        return comprobante;
    }

    public void setComprobante(String comprobante) {
        this.comprobante = comprobante;
    }

    public EstadoAporte getEstado() {
        return estado;
    }

    public void setEstado(EstadoAporte estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

}
