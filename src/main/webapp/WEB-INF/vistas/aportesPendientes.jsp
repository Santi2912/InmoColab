<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.compracolaborativa.modelo.*"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Aporte> aportes = (List<Aporte>) request.getAttribute("aportes");
%>
<h1>Aportes por verificar</h1>
<% if (aportes.isEmpty()) { %>
<div class="card">No hay aportes pendientes de verificación.</div>
<% } else { %>
<div class="tabla">
    <table>
        <tr><th>Aportante</th><th>Grupo</th><th>Monto</th><th>Comprobante</th><th>Fecha</th><th>Decisión</th></tr>
        <% for (Aporte a : aportes) {
               GrupoCompra g = cx.grupos.requerir(a.getIdGrupo());
               Participacion p = cx.aportes.participacion(g, a.getIdParticipacion()); %>
        <tr>
            <td><%= h(cx.usuarios.nombre(p.getIdComprador())) %></td>
            <td><a href="<%= ctx %>/api/grupos/ver?id=<%= g.getId() %>">#<%= g.getId() %></a></td>
            <td><strong><%= Util.dinero(a.getMonto()) %></strong></td>
            <td><%= h(a.getComprobante()) %></td>
            <td><%= Util.fecha(a.getFecha()) %></td>
            <td>
                <form class="en-linea" method="post" action="<%= ctx %>/api/aportes/verificar">
                    <input type="hidden" name="id" value="<%= a.getId() %>">
                    <input type="text" name="motivo" placeholder="Motivo (solo si rechaza)">
                    <button class="btn chico primario" type="submit" name="decision" value="confirmar">Confirmar</button>
                    <button class="btn chico peligro" type="submit" name="decision" value="rechazar">Rechazar</button>
                </form>
            </td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>
