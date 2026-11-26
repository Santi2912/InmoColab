<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.pqr.modelo.*,com.inmocolab.inmuebles.modelo.Inmueble"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Inmueble> inmuebles = (List<Inmueble>) request.getAttribute("inmuebles");
    String tipoSel = Util.val(request, "tipo");
    String inmSel = Util.val(request, "idInmueble");
%>
<h1>Radicar PQR</h1>
<div class="card" style="max-width:820px">
    <form method="post" action="<%= ctx %>/api/pqr/radicar" enctype="multipart/form-data">
        <div class="fila">
            <div><label for="tipo">Tipo</label>
                <select id="tipo" name="tipo" required>
                    <option value="">Selecciona…</option>
                    <% for (TipoPqr t : TipoPqr.values()) { %><option value="<%= t %>"<%= Util.sel(t.name().equals(tipoSel)) %>><%= h(Util.etiqueta(t)) %></option><% } %>
                </select></div>
            <div><label for="idInmueble">Inmueble relacionado (opcional)</label>
                <select id="idInmueble" name="idInmueble">
                    <option value="">Ninguno</option>
                    <% for (Inmueble i : inmuebles) { %><option value="<%= i.getId() %>"<%= Util.sel(String.valueOf(i.getId()).equals(inmSel)) %>>#<%= i.getId() %> · <%= h(i.getDireccion()) %></option><% } %>
                </select></div>
        </div>
        <label for="asunto">Asunto</label>
        <input type="text" id="asunto" name="asunto" maxlength="120" value="<%= Util.val(request, "asunto") %>" required>
        <label for="descripcion">Descripción</label>
        <textarea id="descripcion" name="descripcion" required><%= Util.val(request, "descripcion") %></textarea>
        <label for="adjuntos">Evidencias (opcional: máximo 3 archivos PDF, JPG o PNG de hasta 5 MB)</label>
        <input type="file" id="adjuntos" name="adjuntos" accept=".pdf,.jpg,.jpeg,.png" multiple>
        <p class="suave">Al radicar se genera un número de radicado y la fecha límite de respuesta (15 días hábiles por defecto).</p>
        <div class="acciones">
            <button class="btn primario" type="submit">Radicar</button>
            <a href="<%= ctx %>/api/pqr/lista">Cancelar</a>
        </div>
    </form>
</div>
