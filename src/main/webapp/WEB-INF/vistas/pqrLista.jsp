<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.pqr.modelo.*"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<PQR> pqrs = (List<PQR>) request.getAttribute("pqrs");
%>
<h1><%= h(request.getAttribute("titulo")) %></h1>
<div class="acciones" style="margin:0 0 12px">
    <% if (yo.getRol() != Rol.ADMINISTRADOR) { %><a class="btn primario" href="<%= ctx %>/api/pqr/radicar">Radicar una PQR</a><% } %>
    <% if (yo.getRol() == Rol.ADMINISTRADOR) { %><a class="btn" href="<%= ctx %>/api/pqr/vencidas">Ver PQR vencidas o por vencer</a><% } %>
</div>
<% if (pqrs.isEmpty()) { %>
<div class="card">No hay PQR para mostrar.</div>
<% } else { %>
<div class="tabla">
    <table>
        <tr><th>Radicado</th><th>Tipo</th><th>Asunto</th><% if (yo.getRol() == Rol.ADMINISTRADOR) { %><th>Usuario</th><% } %><th>Radicada</th><th>Fecha límite</th><th>Estado</th><th></th></tr>
        <% for (PQR p : pqrs) { boolean vencida = cx.pqr.estaVencida(p); %>
        <tr class="<%= vencida ? "alerta" : "" %>">
            <td><strong><%= h(p.getRadicado()) %></strong></td>
            <td><%= h(Util.etiqueta(p.getTipo())) %></td>
            <td><%= h(p.getAsunto()) %></td>
            <% if (yo.getRol() == Rol.ADMINISTRADOR) { %><td><%= h(cx.usuarios.nombre(p.getIdUsuario())) %></td><% } %>
            <td><%= Util.fecha(p.getFechaRadicacion()) %></td>
            <td><%= Util.fecha(p.getFechaLimite()) %><%= vencida ? " · vencida" : "" %></td>
            <td><%= Util.insignia(p.getEstado()) %></td>
            <td><a class="btn chico" href="<%= ctx %>/api/pqr/ver?id=<%= p.getId() %>">Abrir</a></td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>
