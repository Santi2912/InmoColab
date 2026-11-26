<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.compracolaborativa.modelo.*,com.inmocolab.inmuebles.modelo.Inmueble"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<GrupoCompra> grupos = (List<GrupoCompra>) request.getAttribute("grupos");
%>
<h1>Mis grupos de compra</h1>
<% if (grupos.isEmpty()) { %>
<div class="card">Aún no perteneces a ningún grupo. Abre un inmueble desde la <a href="<%= ctx %>/api/inmuebles/buscar">búsqueda</a> para crear uno o unirte.</div>
<% } else { %>
<div class="tabla">
    <table>
        <tr><th>Grupo</th><th>Inmueble</th><th>Objetivo</th><th>Recaudado</th><th>Mi participación</th><th>Fecha límite</th><th>Estado</th><th></th></tr>
        <% for (GrupoCompra g : grupos) { Inmueble i = cx.inmuebles.requerir(g.getIdInmueble()); Participacion p = cx.grupos.participacionActiva(g, yo.getId()); %>
        <tr>
            <td>#<%= g.getId() %></td>
            <td><%= h(i.getTipo()) %> · <%= h(i.getCiudad()) %></td>
            <td><%= Util.dinero(g.getMontoObjetivo()) %></td>
            <td><%= Util.dinero(g.getMontoRecaudado()) %></td>
            <td><%= p == null ? "-" : Util.numero(p.getPorcentaje()) + " %" %></td>
            <td><%= Util.fecha(g.getFechaLimite()) %></td>
            <td><%= Util.insignia(g.getEstado()) %></td>
            <td><a class="btn chico" href="<%= ctx %>/api/grupos/ver?id=<%= g.getId() %>">Abrir</a></td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>
