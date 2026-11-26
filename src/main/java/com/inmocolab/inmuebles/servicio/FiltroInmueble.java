package com.inmocolab.inmuebles.servicio;

import java.math.BigDecimal;

/** Criterios de búsqueda de inmuebles (HU-09 y HU-10). */
public class FiltroInmueble {

    public String palabra = "";
    public String ciudad = "";
    public String tipo = "";
    public BigDecimal precioMin;
    public BigDecimal precioMax;
    public BigDecimal areaMin;
    public BigDecimal areaMax;
    public int pagina = 1;
}
