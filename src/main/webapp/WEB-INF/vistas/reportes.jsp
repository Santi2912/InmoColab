<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.reportes.modelo.Reporte,java.net.URLEncoder"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    Map<String, String> tipos = (Map<String, String>) request.getAttribute("tipos");
    @SuppressWarnings("unchecked")
    List<String> ciudades = (List<String>) request.getAttribute("ciudades");
    Reporte r = (Reporte) request.getAttribute("reporte");
    String tipoActual = (String) request.getAttribute("tipoActual");
    String desde = request.getParameter("desde") == null ? "" : request.getParameter("desde");
    String hasta = request.getParameter("hasta") == null ? "" : request.getParameter("hasta");
    String ciudad = request.getParameter("ciudad") == null ? "" : request.getParameter("ciudad");
    String estado = request.getParameter("estado") == null ? "" : request.getParameter("estado");
    boolean usaCiudad = "INMUEBLES".equals(tipoActual) || "NEGOCIACIONES_APORTES".equals(tipoActual);
    boolean usaEstado = "HISTORIAL".equals(tipoActual) || "NEGOCIACIONES_ASIGNADAS".equals(tipoActual);
    String qs = "tipo=" + URLEncoder.encode(tipoActual, "UTF-8") + "&desde=" + URLEncoder.encode(desde, "UTF-8") + "&hasta=" + URLEncoder.encode(hasta, "UTF-8")
            + "&ciudad=" + URLEncoder.encode(ciudad, "UTF-8") + "&estado=" + URLEncoder.encode(estado, "UTF-8");
    double max = 0;
    for (Double v : r.getSerie().values()) { max = Math.max(max, v); }
%>
<h1>Reportes</h1>
<form class="card no-imprimir" method="get" action="<%= ctx %>/api/reportes/ver">
    <div class="fila">
        <div><label for="tipo">Reporte</label>
            <select id="tipo" name="tipo" onchange="this.form.submit()">
                <% for (Map.Entry<String, String> e : tipos.entrySet()) { %><option value="<%= e.getKey() %>"<%= Util.sel(e.getKey().equals(tipoActual)) %>><%= h(e.getValue()) %></option><% } %>
            </select></div>
        <div><label for="desde">Desde</label><input type="date" id="desde" name="desde" value="<%= h(desde) %>"></div>
        <div><label for="hasta">Hasta</label><input type="date" id="hasta" name="hasta" value="<%= h(hasta) %>"></div>
        <% if (usaCiudad) { %>
        <div><label for="ciudad">Ciudad</label>
            <select id="ciudad" name="ciudad"><option value="">Todas</option>
                <% for (String c : ciudades) { %><option<%= Util.sel(c.equalsIgnoreCase(ciudad)) %>><%= h(c) %></option><% } %>
            </select></div>
        <% } %>
        <% if (usaEstado) { %>
        <div><label for="estado">Estado</label>
            <select id="estado" name="estado"><option value="">Todos</option>
                <% String[] estados = "HISTORIAL".equals(tipoActual)
                        ? new String[]{"PENDIENTE", "CONFIRMADO", "RECHAZADO", "EN_DEVOLUCION", "ACEPTADA", "RECHAZADA", "RETIRADA"}
                        : new String[]{"INICIADA", "EN_OFERTA", "ACEPTADA", "EN_FORMALIZACION", "CERRADA", "CANCELADA"};
                   for (String s : estados) { %><option value="<%= s %>"<%= Util.sel(s.equals(estado)) %>><%= h(Util.etiqueta(s)) %></option><% } %>
            </select></div>
        <% } %>
    </div>
    <div class="acciones">
        <button class="btn primario" type="submit">Generar</button>
        <a class="btn" href="<%= ctx %>/api/reportes/exportar?<%= qs %>">Exportar a Excel (CSV)</a>
        <button class="btn" type="button" onclick="window.print()">Imprimir / guardar como PDF</button>
    </div>
</form>

<h2><%= h(r.getTitulo()) %></h2>
<p class="suave">Generado el <%= h(request.getAttribute("generado")) %> por <%= h(yo.getNombre()) %></p>
<div class="grid g4" style="margin-bottom:16px">
    <% for (Map.Entry<String, String> e : r.getIndicadores().entrySet()) { %>
    <div class="tarjeta-numero"><span class="n" style="font-size:1.5rem"><%= h(e.getValue()) %></span><span class="t"><%= h(e.getKey()) %></span></div>
    <% } %>
</div>

<% if (r.getFilas().isEmpty()) { %>
<div class="card">Sin información para los filtros seleccionados.</div>
<% } else { %>
<% if (!r.getSerie().isEmpty() && max > 0) { %>
<div class="card">
    <h2><%= h(r.getTituloSerie()) %></h2>
    <% for (Map.Entry<String, Double> e : r.getSerie().entrySet()) { %>
    <div class="barra-fila">
        <span><%= h(e.getKey()) %></span>
        <div class="b"><div style="width:<%= Math.round(e.getValue() * 100 / max) %>%"></div></div>
        <strong><%= e.getValue() == Math.rint(e.getValue()) ? String.valueOf(e.getValue().longValue()) : Util.dinero(java.math.BigDecimal.valueOf(e.getValue())) %></strong>
    </div>
    <% } %>
</div>
<% } %>
<div class="tabla">
    <table>
        <tr><% for (String col : r.getColumnas()) { %><th><%= h(col) %></th><% } %></tr>
        <% int fila = 0; for (List<String> f : r.getFilas()) { %>
        <tr class="<%= r.getResaltadas().contains(fila++) ? "alerta" : "" %>"><% for (String celda : f) { %><td><%= h(celda) %></td><% } %></tr>
        <% } %>
    </table>
</div>
<% } %>
