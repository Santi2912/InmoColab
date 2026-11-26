
CREATE TABLE IF NOT EXISTS usuario (
  id_usuario SERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  documento VARCHAR(20) NOT NULL UNIQUE,
  correo VARCHAR(120) NOT NULL UNIQUE,
  telefono VARCHAR(20),
  contrasena_hash VARCHAR(255) NOT NULL,
  rol VARCHAR(20) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  fecha_registro TIMESTAMP NOT NULL,
  intentos_fallidos INT NOT NULL DEFAULT 0,      -- prototipo
  bloqueado_hasta TIMESTAMP,                     -- prototipo
  token_verificacion VARCHAR(80)                 -- prototipo
);

CREATE TABLE IF NOT EXISTS notificacion (
  id_notificacion SERIAL PRIMARY KEY,
  id_usuario INT NOT NULL REFERENCES usuario(id_usuario),
  mensaje TEXT NOT NULL,
  enlace VARCHAR(255),                           -- prototipo
  fecha TIMESTAMP NOT NULL,
  leida BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS bitacora (
  id_bitacora SERIAL PRIMARY KEY,
  id_usuario INT NOT NULL REFERENCES usuario(id_usuario),
  accion VARCHAR(60) NOT NULL,
  detalle TEXT,
  fecha TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS parametro (
  clave VARCHAR(60) PRIMARY KEY,
  valor TEXT NOT NULL,
  descripcion VARCHAR(255)
);

-- ===== MS-Inmuebles =====
CREATE TABLE IF NOT EXISTS inmueble (
  id_inmueble SERIAL PRIMARY KEY,
  id_propietario INT NOT NULL REFERENCES usuario(id_usuario),
  id_asesor INT REFERENCES usuario(id_usuario),
  direccion VARCHAR(150) NOT NULL,
  ciudad VARCHAR(60) NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  precio NUMERIC(15,2) NOT NULL,
  area NUMERIC(10,2) NOT NULL,
  descripcion TEXT NOT NULL,
  estado VARCHAR(20) NOT NULL,
  precio_minimo NUMERIC(15,2) NOT NULL,          -- prototipo (oferta mínima)
  visitas INT NOT NULL DEFAULT 0                 -- prototipo
);

CREATE TABLE IF NOT EXISTS fotografia (
  id_foto SERIAL PRIMARY KEY,
  id_inmueble INT NOT NULL REFERENCES inmueble(id_inmueble),
  url VARCHAR(255) NOT NULL,
  orden INT NOT NULL,
  nombre VARCHAR(255),                           -- prototipo
  content_type VARCHAR(60),                      -- prototipo
  datos BYTEA                                    -- prototipo (bytes de la imagen)
);

CREATE TABLE IF NOT EXISTS publicacion (
  id_publicacion SERIAL PRIMARY KEY,
  id_inmueble INT NOT NULL REFERENCES inmueble(id_inmueble),
  id_administrador INT REFERENCES usuario(id_usuario),
  fecha_publicacion TIMESTAMP NOT NULL,
  estado VARCHAR(20) NOT NULL,
  motivo_rechazo TEXT
);

-- ===== MS-CompraColaborativa =====
CREATE TABLE IF NOT EXISTS grupo_compra (
  id_grupo SERIAL PRIMARY KEY,
  id_inmueble INT NOT NULL REFERENCES inmueble(id_inmueble),
  id_creador INT NOT NULL REFERENCES usuario(id_usuario),
  monto_objetivo NUMERIC(15,2) NOT NULL,
  monto_recaudado NUMERIC(15,2) NOT NULL,
  fecha_creacion TIMESTAMP NOT NULL,
  fecha_limite DATE NOT NULL,
  estado VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS invitacion (
  id_invitacion SERIAL PRIMARY KEY,
  id_grupo INT NOT NULL REFERENCES grupo_compra(id_grupo),
  id_invitado INT NOT NULL REFERENCES usuario(id_usuario),
  fecha_envio TIMESTAMP NOT NULL,
  estado VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS participacion (
  id_participacion SERIAL PRIMARY KEY,
  id_grupo INT NOT NULL REFERENCES grupo_compra(id_grupo),
  id_comprador INT NOT NULL REFERENCES usuario(id_usuario),
  porcentaje NUMERIC(5,2) NOT NULL,
  fecha_ingreso TIMESTAMP NOT NULL,
  estado VARCHAR(20) NOT NULL,
  UNIQUE (id_grupo, id_comprador)
);

CREATE TABLE IF NOT EXISTS aporte (
  id_aporte SERIAL PRIMARY KEY,
  id_participacion INT NOT NULL REFERENCES participacion(id_participacion),
  id_verificador INT REFERENCES usuario(id_usuario),
  monto NUMERIC(15,2) NOT NULL,
  fecha TIMESTAMP NOT NULL,
  comprobante_url VARCHAR(255) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  motivo_rechazo TEXT                            -- prototipo
);

-- ===== MS-Negociacion =====
CREATE TABLE IF NOT EXISTS interes (
  id_interes SERIAL PRIMARY KEY,
  id_inmueble INT NOT NULL REFERENCES inmueble(id_inmueble),
  id_comprador INT NOT NULL REFERENCES usuario(id_usuario),
  fecha TIMESTAMP NOT NULL,
  UNIQUE (id_inmueble, id_comprador)
);

CREATE TABLE IF NOT EXISTS negociacion (
  id_negociacion SERIAL PRIMARY KEY,
  id_inmueble INT NOT NULL REFERENCES inmueble(id_inmueble),
  id_asesor INT REFERENCES usuario(id_usuario),
  fecha_inicio TIMESTAMP NOT NULL,
  fecha_fin TIMESTAMP,
  estado VARCHAR(20) NOT NULL,
  ultima_actividad TIMESTAMP NOT NULL            -- prototipo
);

CREATE TABLE IF NOT EXISTS oferta (
  id_oferta SERIAL PRIMARY KEY,
  id_negociacion INT NOT NULL REFERENCES negociacion(id_negociacion),
  id_comprador INT REFERENCES usuario(id_usuario),
  id_grupo INT REFERENCES grupo_compra(id_grupo),
  monto NUMERIC(15,2) NOT NULL,
  fecha TIMESTAMP NOT NULL,
  estado VARCHAR(20) NOT NULL,
  motivo_rechazo TEXT,
  condiciones TEXT,                              -- prototipo
  CHECK ((id_comprador IS NULL) <> (id_grupo IS NULL))
);

CREATE TABLE IF NOT EXISTS observacion (
  id_observacion SERIAL PRIMARY KEY,
  id_negociacion INT NOT NULL REFERENCES negociacion(id_negociacion),
  id_asesor INT NOT NULL REFERENCES usuario(id_usuario),
  texto TEXT NOT NULL,
  fecha TIMESTAMP NOT NULL
);

-- prototipo: historial de estados de la negociación (HU-21)
CREATE TABLE IF NOT EXISTS cambio_estado_negociacion (
  id_negociacion INT NOT NULL REFERENCES negociacion(id_negociacion),
  orden INT NOT NULL,
  estado VARCHAR(20) NOT NULL,
  fecha TIMESTAMP NOT NULL,
  detalle TEXT,
  PRIMARY KEY (id_negociacion, orden)
);

-- ===== MS-PQR =====
CREATE TABLE IF NOT EXISTS pqr (
  id_pqr SERIAL PRIMARY KEY,
  radicado VARCHAR(20) NOT NULL UNIQUE,
  id_usuario INT NOT NULL REFERENCES usuario(id_usuario),
  id_responsable INT REFERENCES usuario(id_usuario),
  tipo VARCHAR(10) NOT NULL,
  asunto VARCHAR(150) NOT NULL,
  descripcion TEXT NOT NULL,
  fecha_radicacion TIMESTAMP NOT NULL,
  fecha_limite DATE NOT NULL,
  estado VARCHAR(20) NOT NULL,
  calificacion SMALLINT,
  id_inmueble INT REFERENCES inmueble(id_inmueble),        -- prototipo
  id_negociacion INT REFERENCES negociacion(id_negociacion), -- prototipo
  fecha_respuesta TIMESTAMP,                     -- prototipo
  fecha_cierre TIMESTAMP,                        -- prototipo
  reabierta BOOLEAN NOT NULL DEFAULT FALSE,      -- prototipo
  CHECK (calificacion BETWEEN 1 AND 5)
);

-- prototipo: la llave es (pqr, orden); url guarda el nombre del archivo y tamano su peso en bytes
CREATE TABLE IF NOT EXISTS adjunto_pqr (
  id_pqr INT NOT NULL REFERENCES pqr(id_pqr),
  orden INT NOT NULL,
  url VARCHAR(255) NOT NULL,
  tamano BIGINT NOT NULL,
  PRIMARY KEY (id_pqr, orden)
);

CREATE TABLE IF NOT EXISTS respuesta_pqr (
  id_respuesta SERIAL PRIMARY KEY,
  id_pqr INT NOT NULL REFERENCES pqr(id_pqr),
  id_autor INT NOT NULL REFERENCES usuario(id_usuario),
  texto TEXT NOT NULL,
  fecha TIMESTAMP NOT NULL
);

-- ===== MS-Comentarios: NO va en PostgreSQL =====
-- Según el diseño usa MongoDB (base NoSQL de documentos). Sus colecciones son:
--   comentarios    { _id, idInmueble, idAutor, texto, fecha, editado, moderado, estado, respuesta,
--                    fechaRespuesta, motivoOcultar, reportes: [ { idUsuario, motivo, fecha } ] }
--   calificaciones { _id, idNegociacion, idComprador, estrellas, texto, fecha }
-- La aplicación las crea sola al guardar el primer documento.

-- ===== MS-Reportes =====
CREATE TABLE IF NOT EXISTS reporte_generado (
  id_reporte SERIAL PRIMARY KEY,
  id_usuario INT NOT NULL REFERENCES usuario(id_usuario),
  tipo VARCHAR(40) NOT NULL,
  filtros TEXT,
  formato VARCHAR(5) NOT NULL,
  fecha TIMESTAMP NOT NULL
);
