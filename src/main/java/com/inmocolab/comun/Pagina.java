package com.inmocolab.comun;

import java.util.ArrayList;
import java.util.List;

/** Resultado paginado. */
public class Pagina<T> {

    private final List<T> items;
    private final int pagina;
    private final int totalPaginas;
    private final int total;

    public Pagina(List<T> todos, int pagina, int tamano) {
        this.total = todos.size();
        this.totalPaginas = Math.max(1, (int) Math.ceil(total / (double) tamano));
        this.pagina = Math.min(Math.max(1, pagina), totalPaginas);
        int desde = (this.pagina - 1) * tamano;
        int hasta = Math.min(total, desde + tamano);
        this.items = new ArrayList<>(todos.subList(desde, hasta));
    }

    public List<T> getItems() {
        return items;
    }

    public int getPagina() {
        return pagina;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public int getTotal() {
        return total;
    }
}
