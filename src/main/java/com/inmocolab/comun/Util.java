package com.inmocolab.comun;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Utilidades de formato y validación compartidas por las vistas y servicios. */
public final class Util {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> ETIQUETAS = new HashMap<>();

    static {
        ETIQUETAS.put("EN_NEGOCIACION", "En negociación");
        ETIQUETAS.put("EN_FORMALIZACION", "En formalización");
        ETIQUETAS.put("EN_TRAMITE", "En trámite");
        ETIQUETAS.put("EN_DEVOLUCION", "En devolución");
        ETIQUETAS.put("EN_OFERTA", "En oferta");
        ETIQUETAS.put("PETICION", "Petición");
        ETIQUETAS.put("ADMINISTRADOR", "Administrador");
    }

    private Util() {
    }

    public static String h(Object o) {
        if (o == null) {
            return "";
        }
        String s = o.toString();
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String fecha(LocalDateTime f) {
        return f == null ? "-" : f.format(FECHA_HORA);
    }

    public static String fecha(LocalDate f) {
        return f == null ? "-" : f.format(FECHA);
    }

    public static String dinero(BigDecimal v) {
        if (v == null) {
            return "-";
        }
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("es", "CO"));
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return "$ " + new DecimalFormat("#,##0", s).format(v);
    }

    public static String numero(BigDecimal v) {
        if (v == null) {
            return "-";
        }
        return v.stripTrailingZeros().toPlainString();
    }

    /** Convierte texto digitado por el usuario en monto; admite 250000000, 250.000.000 o 250000.50. */
    public static BigDecimal monto(String texto, String campo) {
        if (vacio(texto)) {
            throw new NegocioException("Ingresa " + campo + ".");
        }
        String s = texto.trim().replace("$", "").replace(" ", "").replace(",", "");
        if (s.matches("\\d{1,3}(\\.\\d{3})+")) {
            s = s.replace(".", "");
        }
        BigDecimal v;
        try {
            v = new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new NegocioException("El valor de " + campo + " no es válido.");
        }
        if (v.abs().compareTo(new BigDecimal("9999999999999")) > 0) {
            throw new NegocioException("El valor de " + campo + " es demasiado grande.");
        }
        return v.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public static String etiqueta(Object valor) {
        if (valor == null) {
            return "-";
        }
        String clave = valor.toString();
        String e = ETIQUETAS.get(clave);
        if (e != null) {
            return e;
        }
        String[] partes = clave.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(sb.length() == 0 ? Character.toUpperCase(p.charAt(0)) + p.substring(1) : p);
        }
        return sb.toString();
    }

    public static String insignia(Object estado) {
        if (estado == null) {
            return "";
        }
        return "<span class=\"badge e-" + estado + "\">" + h(etiqueta(estado)) + "</span>";
    }

    /** Devuelve (escapado) el valor que el usuario digitó en el último formulario que falló. */
    @SuppressWarnings("unchecked")
    public static String val(HttpServletRequest req, String campo) {
        HttpSession s = req.getSession(false);
        if (s == null) {
            return "";
        }
        Object f = s.getAttribute("form");
        if (f instanceof Map) {
            String v = ((Map<String, String>) f).get(campo);
            return h(v);
        }
        return "";
    }

    /** Valor del formulario o, si no hay, un valor por defecto (ya escapado). */
    public static String val(HttpServletRequest req, String campo, Object defecto) {
        String v = val(req, campo);
        return v.isEmpty() ? h(defecto) : v;
    }

    public static String sel(boolean cond) {
        return cond ? " selected" : "";
    }
}
