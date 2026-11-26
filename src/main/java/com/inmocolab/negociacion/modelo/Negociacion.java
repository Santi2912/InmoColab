package com.inmocolab.negociacion.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Negociacion extends Entidad {

    private long idInmueble;
    private Long idAsesor;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime ultimaActividad;
    private EstadoNegociacion estado;
    private List<Oferta> ofertas = new ArrayList<>();
    private List<Observacion> observaciones = new ArrayList<>();
    private List<CambioEstado> historial = new ArrayList<>();

    public long getIdInmueble() {
        return idInmueble;
    }

    public void setIdInmueble(long idInmueble) {
        this.idInmueble = idInmueble;
    }

    public Long getIdAsesor() {
        return idAsesor;
    }

    public void setIdAsesor(Long idAsesor) {
        this.idAsesor = idAsesor;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public LocalDateTime getUltimaActividad() {
        return ultimaActividad;
    }

    public void setUltimaActividad(LocalDateTime ultimaActividad) {
        this.ultimaActividad = ultimaActividad;
    }

    public EstadoNegociacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoNegociacion estado) {
        this.estado = estado;
    }

    public List<Oferta> getOfertas() {
        return ofertas;
    }

    public void setOfertas(List<Oferta> ofertas) {
        this.ofertas = ofertas;
    }

    public List<Observacion> getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(List<Observacion> observaciones) {
        this.observaciones = observaciones;
    }

    public List<CambioEstado> getHistorial() {
        return historial;
    }

    public void setHistorial(List<CambioEstado> historial) {
        this.historial = historial;
    }

}
