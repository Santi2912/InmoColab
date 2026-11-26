package com.inmocolab.usuarios.modelo;


public class Propietario extends Usuario {


    @Override
    public Rol getRol() {
        return Rol.PROPIETARIO;
    }

}
