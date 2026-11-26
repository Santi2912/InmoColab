<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.inmuebles.modelo.*"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Inmueble> inmuebles = (List<Inmueble>) request.getAttribute("inmuebles");
%>
<h1><%= h(request.getAttribute("titulo")) %></h1>
<% if (yo.getRol() == Rol.PROPIETARIO) { %><p><a class="btn primario" href="<%= ctx %>/api/inmuebles/registrar">Publicar un inmueble</a></p><% } %>
<% if (inmuebles.isEmpty()) { %>
<div class="card">Todavía no hay inmuebles registrados.</div>
<% } else { %>
<div class="tabla">
    <table>
        <tr><th></th><th>Inmueble</th><th>Precio</th><th>Estado</th><th>Publicación</th><th>Visitas</th><th></th></tr>
        <% for (Inmueble i : inmuebles) { Publicacion p = cx.inmuebles.publicacionDe(i.getId()); %>
        <tr>
            <td style="width:90px"><% if (!i.getFotografias().isEmpty()) { %><img src="<%= ctx + i.getFotografias().get(0).getUrl() %>" alt="" style="width:80px;height:56px;object-fit:cover;border-radius:6px"><% } %></td>
            <td><strong><%= h(i.getTipo()) %></strong> · <%= h(i.getCiudad()) %><br><span class="suave"><%= h(i.getDireccion()) %></span></td>
            <td><%= Util.dinero(i.getPrecio()) %></td>
            <td><%= Util.insignia(i.getEstado()) %></td>
            <td><%= p == null ? "-" : Util.insignia(p.getEstado()) %>
                <% if (p != null && p.getEstado() == EstadoPublicacion.RECHAZADA) { %><br><span class="suave"><%= h(p.getMotivoRechazo()) %></span><% } %></td>
            <td><%= i.getVisitas() %></td>
            <td><a class="btn chico" href="<%= ctx %>/api/inmuebles/detalle?id=<%= i.getId() %>">Ver</a></td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>
