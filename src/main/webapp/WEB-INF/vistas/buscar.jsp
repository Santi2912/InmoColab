<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.inmuebles.modelo.*"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    Pagina<Inmueble> resultado = (Pagina<Inmueble>) request.getAttribute("resultado");
    @SuppressWarnings("unchecked")
    List<String> ciudades = (List<String>) request.getAttribute("ciudades");
    @SuppressWarnings("unchecked")
    List<String> tipos = (List<String>) request.getAttribute("tipos");
    String consulta = (String) request.getAttribute("consulta");
    String qCiudad = request.getParameter("ciudad") == null ? "" : request.getParameter("ciudad");
    String qTipo = request.getParameter("tipo") == null ? "" : request.getParameter("tipo");
%>
<h1>Buscar inmuebles</h1>
<form class="card" method="get" action="<%= ctx %>/api/inmuebles/buscar">
    <label for="q">Palabra clave</label>
    <input type="text" id="q" name="q" placeholder="Dirección, barrio, tipo…" value="<%= h(request.getParameter("q")) %>">
    <div class="fila">
        <div><label for="ciudad">Ciudad</label>
            <select id="ciudad" name="ciudad"><option value="">Todas</option>
                <% for (String c : ciudades) { %><option<%= Util.sel(c.equalsIgnoreCase(qCiudad)) %>><%= h(c) %></option><% } %>
            </select></div>
        <div><label for="tipo">Tipo</label>
            <select id="tipo" name="tipo"><option value="">Todos</option>
                <% for (String t : tipos) { %><option<%= Util.sel(t.equalsIgnoreCase(qTipo)) %>><%= h(t) %></option><% } %>
            </select></div>
        <div><label for="precioMin">Precio mínimo</label><input type="text" id="precioMin" name="precioMin" value="<%= h(request.getParameter("precioMin")) %>"></div>
        <div><label for="precioMax">Precio máximo</label><input type="text" id="precioMax" name="precioMax" value="<%= h(request.getParameter("precioMax")) %>"></div>
        <div><label for="areaMin">Área mínima (m²)</label><input type="text" id="areaMin" name="areaMin" value="<%= h(request.getParameter("areaMin")) %>"></div>
        <div><label for="areaMax">Área máxima (m²)</label><input type="text" id="areaMax" name="areaMax" value="<%= h(request.getParameter("areaMax")) %>"></div>
    </div>
    <div class="acciones">
        <button class="btn primario" type="submit">Buscar</button>
        <a class="btn" href="<%= ctx %>/api/inmuebles/buscar">Limpiar filtros</a>
        <span class="suave"><%= resultado.getTotal() %> resultado(s)</span>
    </div>
</form>
<% if (resultado.getItems().isEmpty()) { %>
<div class="card">No se encontraron inmuebles con esos criterios.</div>
<% } else { %>
<div class="grid g3">
    <% for (Inmueble i : resultado.getItems()) {
           int grupos = cx.grupos.abiertosPorInmueble(i.getId()).size();
           String foto = i.getFotografias().isEmpty() ? "" : ctx + i.getFotografias().get(0).getUrl(); %>
    <a class="inmueble" href="<%= ctx %>/api/inmuebles/detalle?id=<%= i.getId() %>">
        <img src="<%= foto %>" alt="Fotografía de <%= h(i.getDireccion()) %>" loading="lazy">
        <div class="cuerpo">
            <div class="precio"><%= Util.dinero(i.getPrecio()) %></div>
            <strong><%= h(i.getTipo()) %> en <%= h(i.getCiudad()) %></strong><br>
            <span class="suave"><%= h(i.getDireccion()) %> · <%= Util.numero(i.getArea()) %> m²</span>
            <div class="acciones">
                <% if (grupos > 0) { %><span class="badge e-ABIERTO"><%= grupos %> grupo(s) abierto(s)</span><% } %>
                <% if (i.getEstado() == EstadoInmueble.EN_NEGOCIACION) { %><%= Util.insignia(i.getEstado()) %><% } %>
            </div>
        </div>
    </a>
    <% } %>
</div>
<% if (resultado.getTotalPaginas() > 1) { %>
<div class="paginacion">
    <% for (int p = 1; p <= resultado.getTotalPaginas(); p++) { %>
    <% if (p == resultado.getPagina()) { %><span class="actual"><%= p %></span>
    <% } else { %><a href="<%= ctx %>/api/inmuebles/buscar?pagina=<%= p %><%= consulta %>"><%= p %></a><% } %>
    <% } %>
</div>
<% } %>
<% } %>
