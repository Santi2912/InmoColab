package com.inmocolab.usuarios.modelo;


public class Comprador extends Usuario {


    @Override
    public Rol getRol() {
        return Rol.COMPRADOR;
    }

}
