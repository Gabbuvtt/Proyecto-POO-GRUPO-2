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
httpx>=0.27.0
httpx2>=2.0.0
```

`httpx` y `httpx2` solo se usan en las pruebas de la API (`TestClient`).

## Dirección de la API

URL base: `http://localhost:8000/api/v1`

El juego Java la lee de `src/main/resources/config.properties` (propiedad `urlBase`).

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

Con el servidor corriendo, abrir `http://localhost:8000/api/v1/salud` en el navegador. Debe devolver:

```json
{"disponible": true, "versionContrato": 2}
```

La documentación de Swagger está en `http://localhost:8000/docs`.

## Ejecutar el juego Java

Requisitos: Java 22 (JDK) y Maven 3.

1. Levantar primero la API (ver la sección anterior). El juego la necesita corriendo en `http://localhost:8000`.

2. Desde la raíz del proyecto, compilar y generar el jar (también corre las pruebas):

   ```
   mvn package
   ```

   El jar incluye Jackson, así que no hace falta nada más para ejecutarlo. En `target/` también queda un `original-...jar` sin dependencias, que no se usa.

3. Ejecutar el juego:

   ```
   java -jar target/expedicion-isla-reliquias-1.0-SNAPSHOT.jar
   ```

Al arrancar, el juego llama a `GET /salud`. Si la API no responde o su `versionContrato` no es `2`, muestra un mensaje y termina.

### Menú principal

```
1. Registrar jugador
2. Nueva partida
3. Cargar partida
4. Consultar estadísticas
5. Reglas y ayuda
0. Salir
```

1. **Registrar jugador**: pide nombre (1 a 80 caracteres) y alias (3 a 20 letras, dígitos, `-` o `_`) y muestra el `jugadorId` que asigna la API. Si el alias ya existe, se muestra el error de la API y se vuelve al menú.
2. **Nueva partida**: pide los alias de los dos jugadores (deben ser distintos), crea la partida con un `requestId` nuevo y muestra el id, el campamento y el orden sorteado. Si falla la red, se reintenta con el mismo `requestId` (hasta 3 veces) para no crear partidas duplicadas.
3. **Cargar partida**: ver abajo.
4. **Consultar estadísticas**: pide un alias y muestra partidas finalizadas, victorias, puntos acumulados y reliquias entregadas.
5. **Reglas y ayuda**: resumen corto de las reglas. Después ofrece descifrar el vector de prueba del Anexo B (`EX-RED-01`, `COFRE-01`, contraseña `ORBITA-7392`) y muestra el JSON que se obtiene. Sirve para la demostración de la sección 14, porque las partidas normales tienen un cofre aleatorio.

Después de cada opción se vuelve al menú. Los errores de la API se muestran con su mensaje.

### Cargar una partida

1. Escribir el alias de uno de los jugadores.
2. Se listan todas sus partidas con id, jugadores, estado y ronda, por ejemplo `EX-001 | ana_ucab, luis_ucab | EN_CURSO | Ronda 2`.
3. Elegir el número de la partida (`0` vuelve al menú).

Una partida `EN_CURSO` sigue exactamente donde se guardó: misma ronda, mismo jugador, mismas acciones restantes, posiciones, inventarios, pistas, templos explorados y segundos jugados. El turno no se reinicia. Una partida `FINALIZADA` solo muestra su resultado.

### Menú del turno

```
1. Mover
2. Explorar templo
3. Entregar reliquias
4. Abrir cofre
5. Consultar tablero, inventario y pistas
6. Guardar
7. Terminar turno
0. Guardar y salir
```

La partida se guarda sola al cerrar cada turno. Si un guardado falla, se bloquean las acciones hasta reintentar con `6` o `0`, y `0` solo sale cuando la API confirma el guardado. La excepción es un conflicto de versiones (`409`, otro proceso guardó la partida antes): ahí `0` vuelve al menú principal sin guardar, avisando que se pierden los cambios desde el último guardado. El tiempo de cada turno se mide en segundos y se envía en el campo `segundosConsumidos` de cada jugador.

## Pruebas

### Python

Desde la carpeta `api`, con el entorno virtual activo:

```
python -m unittest discover -s tests
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

En `postman/` están la colección (`Expedicion.postman_collection.json`) y el entorno (`Expedicion.postman_environment.json`).

La colección tiene los 10 endpoints del contrato v2, cada uno con ejemplos guardados de éxito (200/201) y de error (400, 404, 409, 503). El `PUT /partidas/{id}/estado` incluye `segundosConsumidos` en cada jugador.

1. Importar ambos archivos en Postman.
2. Seleccionar el entorno **Expedición - Local** (`base_url` = `http://localhost:8000/api/v1`).
3. Levantar la API en el puerto `8000` y probar los endpoints en el orden de la colección.

Los ejemplos suponen datos vacíos: los dos primeros jugadores son `J1` y `J2` y la primera partida es `EX-001`. Si ya hay datos guardados, hay que cambiar esos ids en las peticiones. `EX-999` se usa para los ejemplos de partida inexistente.

## Guardado

Los jugadores, partidas y solicitudes (`requestId`) se guardan en `api/datos/expedicion.json`.

- Para no dañar el archivo si falla una escritura, primero se escribe en `api/datos/expedicion.json.tmp` y luego se reemplaza el archivo definitivo.
- Cada partida empieza en revisión `1`. Al guardar con `PUT /partidas/{id}/estado`, se envía `revisionBase`: si coincide con la revisión actual se guarda y sube en `1`; si se reintenta exactamente el mismo guardado devuelve la misma confirmación; si cambia con una base vieja responde `409`.
- El primer guardado con estado `FINALIZADA` (ronda `8` y `0` acciones) cierra el resultado y las estadísticas de esa partida.

## Errores conocidos

- La API funciona con un solo proceso local a la vez; no soporta varias instancias escribiendo al mismo tiempo en `expedicion.json`.
- Si se borra o edita `expedicion.json` a mano con el servidor encendido, hay que reiniciar la API para que recargue los datos.
- El juego Java necesita la API encendida desde el inicio. Si la API se cae a mitad de partida, el guardado queda pendiente hasta que vuelva.
- El tiempo de cada turno se cuenta en segundos completos. Lo que sobra se acumula para el siguiente guardado.
- Si se cierra el juego de golpe (sin `0. Guardar y salir`), se pierde lo jugado desde el último guardado confirmado.
