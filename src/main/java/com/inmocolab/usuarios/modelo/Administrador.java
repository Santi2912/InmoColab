package com.inmocolab.usuarios.modelo;


public class Administrador extends Usuario {


    @Override
    public Rol getRol() {
        return Rol.ADMINISTRADOR;
    }

}
