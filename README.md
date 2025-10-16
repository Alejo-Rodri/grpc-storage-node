# grpc-storage-node

Servidor de nodo para el sistema de almacenamiento distribuido. Expone servicios gRPC para operar sobre el sistema de archivos local del nodo.

## Puerto y despliegue

- Puerto gRPC por defecto: 9090 (localhost:9090)
- Protocolo: gRPC sin TLS por defecto (ajustable en producción)

## Raíz de almacenamiento (node.storage.root)

- Define el directorio raíz bajo el cual se permiten todas las operaciones.
- Las rutas recibidas (relativas o absolutas) se normalizan y deben quedar dentro de este root. Si no, la operación se rechaza con `OUT_OF_ROOT`.
- Configuración por propiedad: `--node.storage.root="<ruta>"`
  - Windows: `E:\\data\\node1`
  - Linux/macOS: `/var/data/node1`
- Valor por defecto: el directorio de trabajo del proceso (`${user.dir}`).

## API gRPC

Servicios expuestos:

- HelloService
  - `sayHello(HelloRequest) -> HelloReply`

- DirectoryService
  - `createDirectory(CreateDirectoryRequest) -> CreateDirectoryResponse`

- FileNodeService (nuevo)
  - MoveOrRename (mover/renombrar)
    - Request: `{ source_path, destination_path }`
    - Response: `{ success, code, message, resulting_path }`
  - DeleteFile (eliminar archivo/directorio)
    - Request: `{ path }`
    - Response: `{ success, code, message }`

Enum `ResponseCode`:
- `OK`, `NOT_FOUND`, `DEST_EXISTS`, `OUT_OF_ROOT`, `PERMISSION_DENIED`, `INVALID_PATH`, `IO_ERROR`.

## Ejemplos con grpcurl

- Move/Rename
```powershell
grpcurl -plaintext -d '{"source_path":"docs/a.txt","destination_path":"docs/b.txt"}' localhost:9090 com.example.grpc.FileNodeService/MoveOrRename
```
- Delete
```powershell
grpcurl -plaintext -d '{"path":"docs/b.txt"}' localhost:9090 com.example.grpc.FileNodeService/DeleteFile
```

## Build y ejecución

Requiere JDK 17 (según `<java.version>17` del POM).

- Compilar
```powershell
Set-Location "E:\Prototipe_2\grpc-storage-node"
mvn -q -DskipTests clean package
```
- Ejecutar
```powershell
java -jar target\grpc-storage-node-0.0.1-SNAPSHOT.jar --node.storage.root="E:\\data\\node1"
```

En Linux/macOS, usa rutas POSIX, por ejemplo:
```bash
java -jar target/grpc-storage-node-0.0.1-SNAPSHOT.jar --node.storage.root="/var/data/node1"
```

## Notas

- Las rutas relativas son recomendadas; se interpretan bajo `node.storage.root`.
- Si envías rutas absolutas, deben estar dentro de `node.storage.root`.
- Eliminar admite archivos y directorios (si un directorio no está vacío, se obtiene `IO_ERROR` con mensaje indicativo).

## CLI (comandos rápidos)

Scripts PowerShell agregados para invocar gRPC sin escribir Java:

- Crear directorio
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\cli-scripts\node-mkdir.ps1 -Path C:\data\test [-GrpcHost <host>] [-GrpcPort <port>]
```

- Mover/renombrar
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\cli-scripts\node-move.ps1 -Source C:\data\a.txt -Destination C:\data\b.txt
```

- Renombrar (alias de move)
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\cli-scripts\node-rename.ps1 -Source C:\data\old.txt -Destination C:\data\new.txt
```

- Eliminar
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\cli-scripts\node-delete.ps1 -Path C:\data\b.txt
```

## Orden recomendado de arranque

1. Arranca primero el servidor del nodo gRPC (este proyecto):
```powershell
mvn spring-boot:run
```
o con JAR (si ya lo empaquetaste):
```powershell
java -jar target\grpc-storage-node-0.0.1-SNAPSHOT.jar
```

2. Luego arranca el servidor principal (SOAP) en el otro proyecto.

Motivo: el servidor principal crea stubs a `localhost:9090` al iniciar (Hello/Directory/FileNode); si el nodo no está disponible, las primeras llamadas podrían fallar o demorar por reintentos.

## Guía paso a paso (PowerShell)

Ejecuta estos comandos para probar rápidamente el nodo y su integración con el main.

### 1) Levantar servicios

- Nodo (desde scripts centralizados):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File E:\Prototipe_2\scripts\start-node.ps1 -Mode mvn
```

- Main server en segundo plano:

```powershell
Start-Process -FilePath "mvn" -ArgumentList "-DskipTests","spring-boot:run" -WorkingDirectory "E:\Prototipe_2\distribuited-storage-main-server" -PassThru
```

### 2) Operaciones locales del nodo

```powershell
Set-Location E:\Prototipe_2\grpc-storage-node

# Crear un directorio
powershell -NoProfile -ExecutionPolicy Bypass -File .\cli-scripts\node-mkdir.ps1 -Path storage\demo

# Crear archivo de prueba
New-Item -Path .\storage\demo\archivo.txt -ItemType File -Force | Out-Null
Set-Content -Path .\storage\demo\archivo.txt -Value "Hola desde el nodo"

# Mover/renombrar
powershell -NoProfile -ExecutionPolicy Bypass -File .\cli-scripts\node-move.ps1 -Source storage\demo\archivo.txt -Destination storage\demo\archivo-renombrado.txt

# Eliminar archivo (y opcionalmente el directorio)
powershell -NoProfile -ExecutionPolicy Bypass -File .\cli-scripts\node-delete.ps1 -Path storage\demo\archivo-renombrado.txt
powershell -NoProfile -ExecutionPolicy Bypass -File .\cli-scripts\node-delete.ps1 -Path storage\demo
```

Verificación rápida:

```powershell
Test-Path .\storage\demo\archivo-renombrado.txt  # True si existe
Test-Path .\storage\demo\archivo.txt             # False si se movió/renombró
Get-Item .\storage\demo\archivo-renombrado.txt | Format-List FullName,Length,LastWriteTime
Get-Content -Raw .\storage\demo\archivo-renombrado.txt
Get-ChildItem .\storage\demo | Select-Object Name,Length,LastWriteTime
Get-FileHash .\storage\demo\archivo-renombrado.txt -Algorithm SHA256
```

## Changelog

- 2025-10-15: Agregados comandos CLI (mkdir, move, rename, delete) bajo `cli-scripts/` y documentación; indicado orden de arranque (nodo primero, luego main).
- 2025-10-15: Se añadió guía paso a paso con comandos para levantar servicios, operar y verificar resultados.

## Trabajo con dos terminales (VS Code)

Para evitar conflictos cuando trabajas con dos ventanas/terminales:

1. Antes de compilar o ejecutar en una terminal, asegúrate de guardar los cambios en el editor y ejecutar `git status` en ambas terminales.
2. Si se agregan/actualizan protos o clases, ejecuta en ambas terminales:
  - `mvn -q -DskipTests clean package`
3. Si una terminal agrega archivos nuevos (por ejemplo, nuevos `.proto`), en la otra terminal ejecuta:
  - `git pull` (si trabajas con remoto) o simplemente vuelve a compilar para regenerar clases.
4. Cuando cambies la propiedad `node.storage.root`, recuerda pasar la misma opción al arrancar en cualquier terminal que ejecute el jar.

