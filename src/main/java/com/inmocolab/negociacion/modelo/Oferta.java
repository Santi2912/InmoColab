package com.inmocolab.negociacion.modelo;

import com.inmocolab.comun.Entidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Oferta extends Entidad {

    private long idNegociacion;
    private Long idComprador;
    private Long idGrupo;
    private BigDecimal monto;
    private String condiciones;
    private LocalDateTime fecha;
    private EstadoOferta estado;
    private String motivoRechazo;

    public long getIdNegociacion() {
        return idNegociacion;
    }

    public void setIdNegociacion(long idNegociacion) {
        this.idNegociacion = idNegociacion;
    }

    public Long getIdComprador() {
        return idComprador;
    }

    public void setIdComprador(Long idComprador) {
        this.idComprador = idComprador;
    }

    public Long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(Long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getCondiciones() {
        return condiciones;
    }

    public void setCondiciones(String condiciones) {
        this.condiciones = condiciones;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public EstadoOferta getEstado() {
        return estado;
    }

    public void setEstado(EstadoOferta estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

}
