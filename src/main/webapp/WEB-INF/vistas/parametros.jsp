<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Parametro> parametros = (List<Parametro>) request.getAttribute("parametros");
%>
<h1>Parámetros del sistema</h1>
<p class="suave">Cambian las reglas sin modificar el software. Cada cambio queda en la bitácora.</p>
<div class="tabla">
    <table>
        <tr><th>Parámetro</th><th>Descripción</th><th>Valor</th></tr>
        <% for (Parametro p : parametros) { %>
        <tr>
            <td><code><%= h(p.getClave()) %></code></td>
            <td><%= h(p.getDescripcion()) %></td>
            <td>
                <form class="en-linea" method="post" action="<%= ctx %>/api/usuarios/parametro">
                    <input type="hidden" name="clave" value="<%= h(p.getClave()) %>">
                    <input type="text" name="valor" value="<%= h(p.getValor()) %>" style="min-width:260px" required>
                    <button class="btn chico" type="submit">Guardar</button>
                </form>
            </td>
        </tr>
        <% } %>
    </table>
</div>
