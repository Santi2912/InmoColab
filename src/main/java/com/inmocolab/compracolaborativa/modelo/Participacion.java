package com.inmocolab.compracolaborativa.modelo;

import com.inmocolab.comun.Entidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Participacion extends Entidad {

    private long idGrupo;
    private long idComprador;
    private BigDecimal porcentaje;
    private LocalDateTime fechaIngreso;
    private EstadoParticipacion estado;
    private List<Aporte> aportes = new ArrayList<>();

    public long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public long getIdComprador() {
        return idComprador;
    }

    public void setIdComprador(long idComprador) {
        this.idComprador = idComprador;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public EstadoParticipacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoParticipacion estado) {
        this.estado = estado;
    }

    public List<Aporte> getAportes() {
        return aportes;
    }

    public void setAportes(List<Aporte> aportes) {
        this.aportes = aportes;
    }

}
