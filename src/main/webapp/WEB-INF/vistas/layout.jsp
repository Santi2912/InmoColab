<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<%
    String vista = (String) request.getAttribute("vista");
    String titulo = (String) request.getAttribute("titulo");
    String[] flash = Flash.tomar(request);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= h(titulo) %> · InmoColab</title>
    <link rel="stylesheet" href="<%= ctx %>/css/estilos.css">
</head>
<body>
<header class="top">
    <div class="barra">
        <a class="marca" href="<%= ctx %>/api/usuarios/inicio">Inmo<span>Colab</span></a>
        <% if (yo != null) { %>
        <nav class="menu">
            <a href="<%= ctx %>/api/usuarios/inicio">Inicio</a>
            <a href="<%= ctx %>/api/inmuebles/buscar">Inmuebles</a>
            <% if (yo.getRol() == Rol.PROPIETARIO) { %>
            <a href="<%= ctx %>/api/inmuebles/mis">Mis inmuebles</a>
            <a href="<%= ctx %>/api/inmuebles/registrar">Publicar</a>
            <% } %>
            <% if (yo.getRol() == Rol.COMPRADOR) { %>
            <a href="<%= ctx %>/api/grupos/mis">Mis grupos</a>
            <% } %>
            <a href="<%= ctx %>/api/negociaciones/mis">Negociaciones</a>
            <a href="<%= ctx %>/api/pqr/lista">PQR</a>
            <a href="<%= ctx %>/api/reportes/ver">Reportes</a>
            <% if (yo.getRol() == Rol.ADMINISTRADOR) { %>
            <a href="<%= ctx %>/api/publicaciones/pendientes">Publicaciones</a>
            <a href="<%= ctx %>/api/aportes/pendientes">Aportes</a>
            <a href="<%= ctx %>/api/comentarios/moderar">Moderación</a>
            <a href="<%= ctx %>/api/pqr/vencidas">PQR vencidas</a>
            <a href="<%= ctx %>/api/usuarios/cuentas">Cuentas</a>
            <a href="<%= ctx %>/api/usuarios/parametros">Parámetros</a>
            <a href="<%= ctx %>/api/usuarios/bitacora">Bitácora</a>
            <% } %>
        </nav>
        <% int noLeidas = cx.usuarios.noLeidas(yo.getId()); %>
        <div class="usuario">
            <a href="<%= ctx %>/api/notificaciones/lista">Avisos<% if (noLeidas > 0) { %><span class="contador"><%= noLeidas %></span><% } %></a>
            <a href="<%= ctx %>/api/usuarios/perfil"><%= h(yo.getNombre()) %> (<%= h(Util.etiqueta(yo.getRol())) %>)</a>
            <a href="<%= ctx %>/api/usuarios/logout">Salir</a>
        </div>
        <% } %>
    </div>
</header>
<main>
    <% if (flash != null) { %>
    <div class="alert <%= flash[0] %>" role="alert"><%= h(flash[1]) %></div>
    <% } %>
    <jsp:include page='<%= "/WEB-INF/vistas/" + vista + ".jsp" %>'/>
</main>
<footer class="pie">InmoColab · Prototipo funcional del segundo entregable · Java (Servlets y JSP) sin frameworks · Datos: <%= h(com.inmocolab.comun.bd.Persistencia.descripcion()) %></footer>
</body>
</html>
<% Flash.limpiarFormulario(request); %>
