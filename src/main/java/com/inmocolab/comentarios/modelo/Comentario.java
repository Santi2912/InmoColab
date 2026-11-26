package com.inmocolab.comentarios.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Comentario extends Entidad {

    private long idInmueble;
    private long idAutor;
    private String texto;
    private LocalDateTime fecha;
    private boolean editado;
    private boolean moderado;
    private EstadoComentario estado;
    private String respuesta;
    private LocalDateTime fechaRespuesta;
    private String motivoOcultar;
    private List<ReporteComentario> reportes = new ArrayList<>();

    public long getIdInmueble() {
        return idInmueble;
    }

    public void setIdInmueble(long idInmueble) {
        this.idInmueble = idInmueble;
    }

    public long getIdAutor() {
        return idAutor;
    }

    public void setIdAutor(long idAutor) {
        this.idAutor = idAutor;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public boolean isEditado() {
        return editado;
    }

    public void setEditado(boolean editado) {
        this.editado = editado;
    }

    public boolean isModerado() {
        return moderado;
    }

    public void setModerado(boolean moderado) {
        this.moderado = moderado;
    }

    public EstadoComentario getEstado() {
        return estado;
    }

    public void setEstado(EstadoComentario estado) {
        this.estado = estado;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public String getMotivoOcultar() {
        return motivoOcultar;
    }

    public void setMotivoOcultar(String motivoOcultar) {
        this.motivoOcultar = motivoOcultar;
    }

    public List<ReporteComentario> getReportes() {
        return reportes;
    }

    public void setReportes(List<ReporteComentario> reportes) {
        this.reportes = reportes;
    }

}
