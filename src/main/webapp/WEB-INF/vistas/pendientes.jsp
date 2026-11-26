<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.inmuebles.modelo.*"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Publicacion> pendientes = (List<Publicacion>) request.getAttribute("pendientes");
%>
<h1>Publicaciones pendientes de validación</h1>
<% if (pendientes.isEmpty()) { %>
<div class="card">No hay publicaciones pendientes.</div>
<% } else { %>
<div class="tabla">
    <table>
        <tr><th>Inmueble</th><th>Propietario</th><th>Precio</th><th>Fotos</th><th>Enviada</th><th>Decisión</th></tr>
        <% for (Publicacion p : pendientes) { Inmueble i = cx.inmuebles.requerir(p.getIdInmueble()); %>
        <tr>
            <td><a href="<%= ctx %>/api/inmuebles/detalle?id=<%= i.getId() %>"><%= h(i.getTipo()) %> · <%= h(i.getCiudad()) %></a><br><span class="suave"><%= h(i.getDireccion()) %></span></td>
            <td><%= h(cx.usuarios.nombre(i.getIdPropietario())) %></td>
            <td><%= Util.dinero(i.getPrecio()) %></td>
            <td><%= i.getFotografias().size() %></td>
            <td><%= Util.fecha(p.getFechaPublicacion()) %></td>
            <td>
                <div class="acciones" style="margin:0">
                    <form class="en-linea" method="post" action="<%= ctx %>/api/publicaciones/aprobar">
                        <input type="hidden" name="id" value="<%= p.getId() %>">
                        <button class="btn chico primario" type="submit">Aprobar</button>
                    </form>
                    <form class="en-linea" method="post" action="<%= ctx %>/api/publicaciones/rechazar">
                        <input type="hidden" name="id" value="<%= p.getId() %>">
                        <input type="text" name="motivo" placeholder="Motivo del rechazo" required>
                        <button class="btn chico peligro" type="submit">Rechazar</button>
                    </form>
                </div>
            </td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>
