package com.inmocolab.usuarios.persistencia;

import com.inmocolab.usuarios.modelo.Parametro;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParametroRepositorio {

    private final Map<String, Parametro> datos = new LinkedHashMap<>();

    public synchronized void guardar(Parametro p) {
        datos.put(p.getClave(), p);
    }

    public synchronized Parametro buscar(String clave) {
        return datos.get(clave);
    }

    public synchronized List<Parametro> todos() {
        return new ArrayList<>(datos.values());
    }
}
