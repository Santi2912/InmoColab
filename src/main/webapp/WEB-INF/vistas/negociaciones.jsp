<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.negociacion.modelo.*,com.inmocolab.inmuebles.modelo.Inmueble"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Negociacion> lista = (List<Negociacion>) request.getAttribute("negociaciones");
%>
<h1><%= h(request.getAttribute("titulo")) %></h1>
<% if (lista.isEmpty()) { %>
<div class="card">No tienes negociaciones todavía. Manifiesta interés o haz una oferta desde el detalle de un inmueble.</div>
<% } else { %>
<div class="tabla">
    <table>
        <tr><th>N.°</th><th>Inmueble</th><th>Estado</th><th>Ofertas</th><th>Asesor</th><th>Inicio</th><th>Última actividad</th><th></th></tr>
        <% for (Negociacion n : lista) { Inmueble i = cx.inmuebles.requerir(n.getIdInmueble()); %>
        <tr>
            <td>#<%= n.getId() %></td>
            <td><%= h(i.getTipo()) %> · <%= h(i.getCiudad()) %><br><span class="suave"><%= h(i.getDireccion()) %></span></td>
            <td><%= Util.insignia(n.getEstado()) %></td>
            <td><%= n.getOfertas().size() %></td>
            <td><%= h(cx.usuarios.nombre(n.getIdAsesor())) %></td>
            <td><%= Util.fecha(n.getFechaInicio()) %></td>
            <td><%= Util.fecha(n.getUltimaActividad()) %></td>
            <td><a class="btn chico" href="<%= ctx %>/api/negociaciones/ver?id=<%= n.getId() %>">Abrir</a></td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>
