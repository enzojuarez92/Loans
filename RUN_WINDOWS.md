# Ejecutar AppLoans en Windows

## Antes de actualizar un cliente

1. Cerrá AppLoans en la PC del cliente.
2. Hacé una copia de seguridad del archivo de datos:

   ```powershell
   $fecha = Get-Date -Format 'yyyyMMdd-HHmmss'
   Copy-Item "$env:USERPROFILE\.apploans\apploans.db" "C:\Backups\apploans-$fecha.db"
   ```

   Creá `C:\Backups` si todavía no existe. No copies el archivo mientras la aplicación esté abierta.
3. Reemplazá el JAR o la carpeta de la aplicación por la versión nueva. La base sigue en `%USERPROFILE%\.apploans\apploans.db`; no se reemplaza ni se mueve.

Al primer inicio, esta versión agrega solamente dos elementos técnicos compatibles: la columna `sales_payments.paid_at` si falta y la tabla de trazabilidad de pagos nuevos. Conserva todos los clientes, préstamos, cuotas, cobros, importes y estados ya existentes.

## Ejecutar la aplicación compilada

Instalá [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) en la PC si todavía no tiene Java. Después, desde esta carpeta:

```powershell
.\run-windows.bat
```

El script ejecuta `target\apploans-1.0.0.jar`.

También se puede ejecutar manualmente:

```powershell
java -jar .\target\apploans-1.0.0.jar
```

## Compilar desde el código fuente

Con JDK 21 y Maven 3.9 o superior instalados y disponibles en `PATH`:

```powershell
mvn -DskipTests package
```

El archivo listo queda en `target\apploans-1.0.0.jar`.

## Verificación recomendada antes de entregar

En una copia de la base de un cliente, comprobá:

1. Inicio de sesión con el usuario habitual.
2. Totales y cuotas de una ficha de préstamo conocida.
3. Dashboard y reportes generales.
4. Un cobro pequeño de prueba y su reversión, si el proceso comercial lo permite.

La contraseña existente no cambia al actualizar. Cuando cada usuario inicia sesión correctamente por primera vez, la aplicación protege su contraseña anterior mediante PBKDF2 sin modificar su usuario ni sus permisos.
