package com.inmocolab.pqr.modelo;


public class Adjunto {

    private String nombre;
    private long tamano;

    public Adjunto() {
    }

    public Adjunto(String nombre, long tamano) {
        this.nombre = nombre;
        this.tamano = tamano;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getTamano() {
        return tamano;
    }

    public void setTamano(long tamano) {
        this.tamano = tamano;
    }

}
