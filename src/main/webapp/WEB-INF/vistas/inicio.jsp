<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    Map<String, String[]> resumen = (Map<String, String[]>) request.getAttribute("resumen");
%>
<h1>Hola, <%= h(yo.getNombre()) %></h1>
<p class="suave">Ingresaste como <strong><%= h(Util.etiqueta(yo.getRol())) %></strong>. Este es el resumen de tu actividad.</p>
<div class="grid g4">
    <% for (Map.Entry<String, String[]> e : resumen.entrySet()) { %>
    <a class="tarjeta-numero" href="<%= ctx + e.getValue()[1] %>">
        <span class="n"><%= h(e.getValue()[0]) %></span>
        <span class="t"><%= h(e.getKey()) %></span>
    </a>
    <% } %>
</div>
<div class="card" style="margin-top:16px">
    <h2>¿Qué quieres hacer?</h2>
    <div class="acciones">
        <a class="btn primario" href="<%= ctx %>/api/inmuebles/buscar">Buscar inmuebles</a>
        <% if (yo.getRol() == Rol.PROPIETARIO) { %><a class="btn" href="<%= ctx %>/api/inmuebles/registrar">Publicar un inmueble</a><% } %>
        <% if (yo.getRol() == Rol.COMPRADOR) { %><a class="btn" href="<%= ctx %>/api/grupos/mis">Mis grupos de compra</a><% } %>
        <% if (yo.getRol() != Rol.ADMINISTRADOR) { %><a class="btn" href="<%= ctx %>/api/pqr/radicar">Radicar una PQR</a><% } %>
        <a class="btn" href="<%= ctx %>/api/reportes/ver">Ver reportes</a>
    </div>
</div>
