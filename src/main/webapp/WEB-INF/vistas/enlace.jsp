<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<%
    String mensaje = (String) request.getAttribute("mensajeEnlace");
    String enlace = (String) request.getAttribute("enlace");
%>
<div class="centro">
    <div class="card">
        <h1><%= h(request.getAttribute("titulo")) %></h1>
        <p><%= h(mensaje) %></p>
        <% if (enlace != null) { %>
        <p><a class="btn primario" href="<%= ctx + enlace %>">Abrir enlace del correo</a></p>
        <% } %>
        <p><a href="<%= ctx %>/api/usuarios/login">Volver a iniciar sesión</a></p>
    </div>
</div>
