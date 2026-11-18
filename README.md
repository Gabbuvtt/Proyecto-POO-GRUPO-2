# Expedición: La Isla de las Reliquias

Proyecto de Programación Orientada a Objetos (UCAB), Fase 1. Consta de dos partes:

- `api/`: API en Python (FastAPI) que genera las partidas, los templos, las pistas y el cofre cifrado.
- `src/`: aplicación Java de consola que contiene las reglas del juego y abre el cofre.

## Integrantes

- Gianfranco Cala
- Gabriel Mejía
- Sebastián Serrano
- Sebastián González

## Requisitos

- Python 3.10 o superior.
- Java 22 (JDK).
- Maven 3 (puede ser el que trae IntelliJ IDEA).

Las librerías de Python están en `api/requirements.txt`:

```
fastapi>=0.100.0
uvicorn[standard]>=0.20.0
pydantic>=2.0.0
cryptography>=41.0.0
```

## Dirección de la API

URL base: `http://localhost:8000/api/v1`

La documentación interactiva (Swagger) está en `http://localhost:8000/docs`.

## Levantar la API en local

Todos los comandos se ejecutan dentro de la carpeta `api`.

1. Crear el entorno virtual:

   ```
   cd api
   python -m venv venv
   ```

2. Activarlo:

   - Windows (PowerShell): `venv\Scripts\Activate.ps1`

     Si PowerShell no deja ejecutar el script, primero hay que correr una vez:
     `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

   - Windows (cmd): `venv\Scripts\activate.bat`
   - Linux o macOS: `source venv/bin/activate`

3. Instalar las dependencias:

   ```
   pip install -r requirements.txt
   ```

4. Iniciar el servidor:

   ```
   uvicorn main:app --reload --port 8000
   ```

### Verificación con /salud

Con el servidor encendido, abre en el navegador o consulta con `curl`:

```
GET http://localhost:8000/api/v1/salud
```

Debe responder `200 OK` con:

```json
{"disponible": true, "versionContrato": 2}
```

La documentación interactiva de Swagger UI (con los 10 endpoints, esquemas y códigos de respuesta) está disponible en `http://localhost:8000/docs`.

## Ejecutar el juego Java

Requisitos: Java 22 (JDK) y Maven 3.

1. Levantar primero la API (ver la sección anterior). El juego la necesita corriendo en `http://localhost:8000`.

2. Desde la raíz del proyecto, compilar y generar el jar (también corre las pruebas):

   ```
   mvn package
   ```

3. Ejecutar el juego:

   ```
   java -jar target/expedicion-isla-reliquias-1.0-SNAPSHOT.jar
   ```

## Pruebas

### Python

Desde la carpeta `api`, con el entorno virtual activo:

```
python -m unittest discover tests
```

### Java

Desde la raíz del proyecto (donde está `pom.xml`):

```
mvn test
```

Si Maven no está en el PATH se puede usar el de IntelliJ, por ejemplo:

```
"C:\Program Files\JetBrains\IntelliJ IDEA <version>\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" test
```

## Reglas del juego (resumen)

- **Tablero**: 5x5, columnas A–E y filas 1–5 (A1 es la esquina superior izquierda). Tiene 1 campamento (C), 1 cofre (X), 4 templos (T), 4 pantanos (P) y 15 senderos (S). Los dos jugadores empiezan en C.
- **Turnos**: la API sortea el orden de los jugadores. Cada turno da 3 puntos de acción; los que no se usan se pierden. La partida dura 8 rondas.
- **Acciones**:
  - Moverse a una casilla vecina (sin diagonales): cuesta 1; entrar en un pantano cuesta 2.
  - Explorar un templo y recoger su reliquia: cuesta 1.
  - Entregar todas las reliquias en C: cuesta 1.
  - Intentar abrir el cofre en X: cuesta 1.
  - Consultar, guardar o terminar el turno: no cuesta.
- **Reliquias**: los templos tienen reliquias de 1, 2, 2 y 3 puntos. Dos templos tienen además una pista, que ven ambos jugadores. El inventario tiene capacidad para 2 reliquias y solo se vacía entregando en C.
- **Cofre**: está cifrado con AES-GCM y una clave derivada con PBKDF2. La contraseña es `pista1-pista2` (por ejemplo `ORBITA-7392`). Para intentar abrirlo hay que estar en X, conocer las dos pistas y tener espacio. Una contraseña con formato válido pero incorrecta cuesta 1 punto; un formato inválido o un paquete corrupto no cuesta. Al abrirlo se obtiene la reliquia especial de 4 puntos, que solo suma cuando se entrega en C. Abrir el cofre no termina la partida.
- **Ganador**: al terminar la ronda 8 gana quien tenga más puntos entregados. Si empatan, gana quien entregó más reliquias; si también empatan, comparten la victoria. Las reliquias que quedan en el inventario no suman.

## Postman

En la carpeta `postman/` se incluyen la colección y el entorno listos para importar:

1. Abrir Postman y hacer clic en **Import**.
2. Seleccionar `postman/Expedicion.postman_collection.json` y `postman/Expedicion.postman_environment.json`.
3. En la esquina superior derecha de Postman, seleccionar el entorno **Expedición - Local** (define `base_url = http://localhost:8000/api/v1`).
4. Con la API corriendo en el puerto `8000`, ejecutar los requests en orden (desde `GET /salud` hasta `GET /jugadores/{id}/estadisticas`). Cada endpoint incluye ejemplos guardados de respuestas exitosas y de error.

## Guardado

La API persiste jugadores, partidas y solicitudes (`requestId`) en un único archivo JSON: `api/datos/expedicion.json`.

- **Escritura atómica**: cada cambio se escribe primero en `api/datos/expedicion.json.tmp` con `flush` y `os.fsync`, y luego reemplaza al archivo definitivo con `os.replace`, evitando archivos truncados si ocurre un fallo.
- **Control optimista por revisión (`revisionBase`)**: toda partida inicia en `revision = 1`. Al guardar con `PUT /api/v1/partidas/{id}/estado`, la API verifica que `revisionBase` coincida con la revisión actual; si coincide, guarda el snapshot y aumenta la revisión en `1`.
- **Reintento idempotente**: si el cliente reenvía exactamente el mismo `estado` con el mismo `revisionBase` del último guardado confirmado, la API devuelve la confirmación previa sin duplicar cambios ni subir la revisión.
- **Conflictos (`409 Conflict`)**: enviar una `revisionBase` distinta a la actual (o reutilizar una base anterior con otro estado) devuelve `409 REVISION_INCOMPATIBLE`.
- **Cierre de partida**: el primer guardado con estado `FINALIZADA` (ronda `8` y `accionesRestantes = 0`) fija el resultado y las estadísticas; intentos posteriores de modificarla devuelven `409 PARTIDA_FINALIZADA` (salvo reintento idéntico).

## Errores conocidos

- La API está diseñada para ejecutarse como un único proceso local (`uvicorn main:app --port 8000`) y una sola partida activa por sesión; no soporta múltiples instancias de servidor escribiendo simultáneamente sobre el mismo archivo JSON.
- Si se borra o edita manualmente `api/datos/expedicion.json` mientras el servidor está corriendo, es necesario reiniciar el servidor para recargar el estado en memoria.
