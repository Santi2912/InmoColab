package com.inmocolab.comun;

/** Base de todas las entidades con identificador. */
public abstract class Entidad {

    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
