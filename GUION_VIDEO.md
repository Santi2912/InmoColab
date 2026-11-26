# Guion del video (máx. 10 minutos)

Reemplacen `[Integrante X]` por los nombres reales. Cada uno habla en su parte, para que se vea la participación de todos.
Antes de grabar: base de datos limpia (datos de ejemplo), app abierta en `http://localhost:8080/InmoColab/`.

## 1. Presentación (0:00 – 1:00) · [todos]
**Pantalla:** cada integrante en cámara, o la pantalla de login de la app.

- [Integrante 1]: "Hola, soy [nombre]. Trabajé en [módulo o parte]."
- [Integrante 2]: "Hola, soy [nombre]. Trabajé en [módulo o parte]."
- [Integrante 3]: igual.
- Cualquiera: "Somos el grupo de InmoColab, una plataforma para la compra y venta colaborativa de inmuebles. Hecha en Java con Servlets y JSP, sin frameworks, con PostgreSQL y MongoDB."

## 2. El software funcionando (1:00 – 4:30)

### 2.1 Login y panel (≈ 20 s) · [Integrante 1]
**Pantalla:** login → panel de inicio. Entra como `admin@inmocolab.com` / `Admin123`.
- "Hay cuatro roles: administrador, propietario, comprador y asesor. Cada uno ve un menú distinto."
- "Las contraseñas están cifradas y tras tres intentos fallidos la cuenta se bloquea 15 minutos."

### 2.2 Propietario publica un inmueble (≈ 35 s) · [Integrante 1]
**Pantalla:** cierra sesión, entra como `propietario@inmocolab.com` / `Demo1234` → **Publicar**.
- "El propietario registra el inmueble con dirección, ciudad, tipo, precio, área y mínimo tres fotos."
- Intenta enviar con 2 fotos: "El sistema valida las reglas: exige mínimo tres fotos JPG o PNG." Sube 3 y envía.
- "Queda *Pendiente de validación*."

### 2.3 Administrador valida (≈ 20 s) · [Integrante 2]
**Pantalla:** entra como admin → **Publicaciones** → **Aprobar**.
- "El administrador revisa y aprueba. Si rechaza, debe escribir el motivo. El propietario recibe una notificación."

### 2.4 Compra colaborativa (≈ 80 s) · [Integrante 2]
**Pantalla:** entra como `comprador@inmocolab.com` / `Demo1234` → **Mis grupos** → grupo #1.
- "Aquí se compra entre varios. Este grupo tiene un monto objetivo y cada integrante aporta."
- **Registrar mi aporte:** monto `54000000`, comprobante `transferencia.pdf`.
  "El sistema no deja superar el saldo pendiente del objetivo."
- Cierra sesión, entra como admin → **Aportes** → **Confirmar** los aportes pendientes.
  "El administrador verifica cada aporte. Al confirmar se recalcula el porcentaje de participación de cada integrante."
- Vuelve como comprador al grupo: "Ya está completo: llegó al monto objetivo."

### 2.5 Oferta y cierre (≈ 50 s) · [Integrante 3]
**Pantalla:** comprador → detalle del inmueble del grupo → **Ofertar como** grupo, monto `175000000`.
- "La oferta de grupo solo se permite si el grupo está completo y su monto cubre lo ofertado."
- Entra como propietario → **Negociaciones** → **Aceptar** → **Cerrar negociación**.
  "Al aceptar, las otras ofertas se rechazan. Al cerrar, el inmueble queda vendido."

### 2.6 PQR, comentarios y reportes (≈ 45 s) · [Integrante 3]
**Pantalla:** **PQR** → **Radicar**; luego **Reportes**.
- "Cualquier usuario radica una petición, queja o reclamo. Se genera un número de radicado y una fecha límite de 15 días hábiles."
- "Hay moderación de comentarios: tres reportes ocultan un comentario hasta que el administrador lo revisa."
- **Reportes:** "Cada rol ve solo los suyos y se pueden exportar a Excel (CSV)."

## 3. Estructura de carpetas (4:30 – 6:00) · [Integrante 1]
**Pantalla:** NetBeans → Projects → Source Packages → `com.inmocolab`.

- "La arquitectura es de microservicios con un API Gateway. Aquí está el paquete `gateway`."
- "Hay un paquete por microservicio: usuarios, inmuebles, compra colaborativa, negociación, PQR, comentarios y reportes."
- Abre `inmuebles`: "Cada uno tiene las mismas cuatro capas. `presentacion` son los servlets, `servicio` tiene las reglas de negocio, `modelo` las entidades y `persistencia` los repositorios."
- "Cada capa solo usa la de abajo: la presentación nunca habla con la base de datos."
- Abre `Web Pages → WEB-INF → vistas`: "Aquí una JSP por pantalla."
- Abre `comun`: "Aquí lo compartido, y `comun/bd` tiene la conexión a las bases de datos."

## 4. Patrones implementados (6:00 – 7:30) · [Integrante 2]
**Pantalla:** abre cada archivo en este orden (`Ctrl+Shift+O` para ir rápido).

1. `FiltroSesion.java` → "**Intercepting Filter**. Toda petición pasa por aquí: exige sesión y al final guarda en la base de datos."
2. `APIGatewayServlet.java` → "**API Gateway y Front Controller**. Recibe todo `/api`, revisa el rol y reenvía al microservicio correcto."
3. `ServletBase.java` → "**Template Method**. Define el esqueleto común de todos los controladores."
4. `InmuebleServlet.java` + una JSP → "**MVC**. El servlet es el controlador, el servicio y el modelo son el dominio y la JSP es la vista."
5. `InmuebleServicio.java` (método `registrar`) → "Capa de servicio: aquí están las reglas, como el mínimo de tres fotos."
6. `InmuebleRepositorio.java` → "**Repository o DAO**. El servicio no sabe cómo se guardan los datos."
7. `Contexto.java` → "**Singleton e inyección de dependencias manual**. Crea y conecta los servicios sin usar ningún framework."

## 5. Manejo de bases de datos (7:30 – 9:15)

### 5.1 PostgreSQL, base relacional (≈ 60 s) · [Integrante 3]
**Pantalla:** NetBeans → Services → conexión a PostgreSQL → `public → Tables`.
- "Los datos de usuarios, inmuebles, aportes, negociaciones y PQR están en PostgreSQL, con JDBC puro, sin ORM."
- Clic derecho en `inmueble` → **View Data**. "Aquí están los inmuebles que acabamos de crear."
- En el código (`comun/bd/Mapeos.java`): "**Data Mapper**: cada tabla se relaciona con su objeto. Y `Persistencia.java`: **Unit of Work**, guarda los cambios de la petición en una sola transacción."

### 5.2 MongoDB, base NoSQL (≈ 60 s) · [Integrante 3]
**Pantalla:** MongoDB Compass → `inmocolab` → `comentarios`.
- "Los comentarios y calificaciones, que son el microservicio MS-Comentarios, usan MongoDB, una base NoSQL de documentos."
- Muestra un documento: "Cada comentario es un documento. Fíjense que los reportes van **incrustados** como un arreglo dentro del mismo documento. En SQL serían otra tabla."
- Publica un comentario en la app y refresca Compass: "Aparece el documento nuevo."
- En el código (`PersistenciaMongo.java`): "Aquí se convierte el objeto en un documento y se guardan solo los cambios."

## 6. Cierre (9:15 – 9:45) · [todos]
- "En resumen, InmoColab implementa una arquitectura de microservicios con API Gateway y capas por servicio, usando SQL con PostgreSQL y NoSQL con MongoDB, sin ningún framework. Gracias."

---

### Datos útiles para la demo
- El grupo #1 tiene objetivo de $180.000.000. Con el aporte de $54.000.000 y los dos aportes confirmados llega al objetivo.
- La oferta mínima del inmueble del grupo es $168.750.000; ofertar $175.000.000 funciona.
- Cuentas: admin `admin@inmocolab.com` / `Admin123`; las demás (`propietario@`, `comprador@`, `comprador2@`, `asesor@inmocolab.com`) con `Demo1234`.

### Si se pasan de tiempo, recorten
1. La PQR y los reportes (2.6).
2. La explicación de las reglas de validación en 2.2.
