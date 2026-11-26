<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.usuarios.modelo.Notificacion"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Notificacion> lista = (List<Notificacion>) request.getAttribute("notificaciones");
%>
<h1>Notificaciones</h1>
<% if (lista.isEmpty()) { %>
<div class="card">No tienes notificaciones.</div>
<% } else { %>
<form method="post" action="<%= ctx %>/api/notificaciones/leerTodas"><button class="btn chico" type="submit">Marcar todas como leídas</button></form>
<div class="tabla" style="margin-top:10px">
    <table>
        <tr><th>Fecha</th><th>Mensaje</th><th></th></tr>
        <% for (Notificacion n : lista) { %>
        <tr style="<%= n.isLeida() ? "" : "background:#fff8e1;font-weight:600" %>">
            <td><%= Util.fecha(n.getFecha()) %></td>
            <td><%= h(n.getMensaje()) %></td>
            <td><a class="btn chico" href="<%= ctx %>/api/notificaciones/abrir?id=<%= n.getId() %>"><%= n.isLeida() ? "Abrir" : "Leer" %></a></td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>
