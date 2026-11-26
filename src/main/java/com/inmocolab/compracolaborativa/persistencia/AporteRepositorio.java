package com.inmocolab.compracolaborativa.persistencia;

import com.inmocolab.comun.RepositorioMemoria;
import com.inmocolab.compracolaborativa.modelo.Aporte;
import com.inmocolab.compracolaborativa.modelo.EstadoAporte;
import java.util.List;

/** Índice global de aportes (los mismos objetos viven dentro de su Participacion). */
public class AporteRepositorio extends RepositorioMemoria<Aporte> {

    public List<Aporte> porEstado(EstadoAporte estado) {
        return filtrar(a -> a.getEstado() == estado);
    }
}
