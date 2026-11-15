# Expedición: La Isla de las Reliquias

Proyecto de Programación Orientada a Objetos (UCAB), Fase 1. Consta de dos partes:

- `api/`: API en Python (FastAPI) que genera las partidas, los templos, las pistas y el cofre cifrado.
- `src/`: aplicación Java de consola que contiene las reglas del juego y abre el cofre.

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

Pendiente.

## Ejecutar el juego Java

Pendiente.

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

## Guardado

Pendiente.

## Errores conocidos

Pendiente.
