package com.inmocolab.usuarios.modelo;


public class Asesor extends Usuario {


    @Override
    public Rol getRol() {
        return Rol.ASESOR;
    }

}
