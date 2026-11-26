package com.inmocolab.pqr.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PQR extends Entidad {

    private String radicado;
    private long idUsuario;
    private Long idResponsable;
    private TipoPqr tipo;
    private String asunto;
    private String descripcion;
    private Long idInmueble;
    private Long idNegociacion;
    private LocalDateTime fechaRadicacion;
    private LocalDateTime fechaRespuesta;
    private LocalDateTime fechaCierre;
    private LocalDate fechaLimite;
    private EstadoPqr estado;
    private Integer calificacion;
    private boolean reabierta;
    private List<RespuestaPQR> respuestas = new ArrayList<>();
    private List<Adjunto> adjuntos = new ArrayList<>();

    public String getRadicado() {
        return radicado;
    }

    public void setRadicado(String radicado) {
        this.radicado = radicado;
    }

    public long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdResponsable() {
        return idResponsable;
    }

    public void setIdResponsable(Long idResponsable) {
        this.idResponsable = idResponsable;
    }

    public TipoPqr getTipo() {
        return tipo;
    }

    public void setTipo(TipoPqr tipo) {
        this.tipo = tipo;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getIdInmueble() {
        return idInmueble;
    }

    public void setIdInmueble(Long idInmueble) {
        this.idInmueble = idInmueble;
    }

    public Long getIdNegociacion() {
        return idNegociacion;
    }

    public void setIdNegociacion(Long idNegociacion) {
        this.idNegociacion = idNegociacion;
    }

    public LocalDateTime getFechaRadicacion() {
        return fechaRadicacion;
    }

    public void setFechaRadicacion(LocalDateTime fechaRadicacion) {
        this.fechaRadicacion = fechaRadicacion;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public EstadoPqr getEstado() {
        return estado;
    }

    public void setEstado(EstadoPqr estado) {
        this.estado = estado;
    }

    public Integer getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }

    public boolean isReabierta() {
        return reabierta;
    }

    public void setReabierta(boolean reabierta) {
        this.reabierta = reabierta;
    }

    public List<RespuestaPQR> getRespuestas() {
        return respuestas;
    }

    public void setRespuestas(List<RespuestaPQR> respuestas) {
        this.respuestas = respuestas;
    }

    public List<Adjunto> getAdjuntos() {
        return adjuntos;
    }

    public void setAdjuntos(List<Adjunto> adjuntos) {
        this.adjuntos = adjuntos;
    }

}
