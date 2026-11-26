<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.inmuebles.modelo.Inmueble"%>
<%@include file="_comun.jspf"%>
<%
    Inmueble i = (Inmueble) request.getAttribute("inmueble");
    boolean editar = i != null;
    @SuppressWarnings("unchecked")
    List<String> ciudades = (List<String>) request.getAttribute("ciudades");
    @SuppressWarnings("unchecked")
    List<String> tipos = (List<String>) request.getAttribute("tipos");
    String ciudadSel = Util.val(request, "ciudad", editar ? i.getCiudad() : "");
    String tipoSel = Util.val(request, "tipo", editar ? i.getTipo() : "");
%>
<h1><%= editar ? "Editar inmueble" : "Publicar inmueble" %></h1>
<div class="card" style="max-width:820px">
    <form method="post" action="<%= ctx %>/api/inmuebles/<%= editar ? "editar" : "registrar" %>"<%= editar ? "" : " enctype=\"multipart/form-data\"" %>>
        <% if (editar) { %><input type="hidden" name="id" value="<%= i.getId() %>"><% } %>
        <label for="direccion">Dirección</label>
        <input type="text" id="direccion" name="direccion" maxlength="150" value="<%= Util.val(request, "direccion", editar ? i.getDireccion() : "") %>" required>
        <div class="fila">
            <div><label for="ciudad">Ciudad</label>
                <select id="ciudad" name="ciudad" required>
                    <option value="">Selecciona…</option>
                    <% for (String c : ciudades) { %><option<%= Util.sel(h(c).equals(ciudadSel)) %>><%= h(c) %></option><% } %>
                </select></div>
            <div><label for="tipo">Tipo de inmueble</label>
                <select id="tipo" name="tipo" required>
                    <option value="">Selecciona…</option>
                    <% for (String t : tipos) { %><option<%= Util.sel(h(t).equals(tipoSel)) %>><%= h(t) %></option><% } %>
                </select></div>
            <div><label for="precio">Precio de venta (pesos)</label>
                <input type="text" id="precio" name="precio" value="<%= Util.val(request, "precio", editar ? Util.numero(i.getPrecio()) : "") %>" required></div>
            <div><label for="area">Área (m²)</label>
                <input type="text" id="area" name="area" value="<%= Util.val(request, "area", editar ? Util.numero(i.getArea()) : "") %>" required></div>
            <div><label for="precioMinimo">Oferta mínima aceptada (opcional)</label>
                <input type="text" id="precioMinimo" name="precioMinimo" placeholder="Por defecto 70 % del precio"
                       value="<%= Util.val(request, "precioMinimo", editar ? Util.numero(i.getPrecioMinimo()) : "") %>"></div>
        </div>
        <label for="descripcion">Descripción</label>
        <textarea id="descripcion" name="descripcion" required><%= Util.val(request, "descripcion", editar ? i.getDescripcion() : "") %></textarea>
        <% if (!editar) { %>
        <label for="fotos">Fotografías (mínimo 3, JPG o PNG, máximo 5 MB cada una)</label>
        <input type="file" id="fotos" name="fotos" accept="image/jpeg,image/png" multiple required>
        <p class="suave">Al enviar, la publicación queda <strong>Pendiente de validación</strong> por el administrador.</p>
        <% } %>
        <div class="acciones">
            <button class="btn primario" type="submit"><%= editar ? "Guardar cambios" : "Enviar a validación" %></button>
            <a href="<%= ctx %>/api/inmuebles/mis">Cancelar</a>
        </div>
    </form>
</div>
