@echo off
setlocal

where java >nul 2>nul
if errorlevel 1 (
    echo No se encontro Java 21 o superior en PATH.
    echo Instala Eclipse Temurin 21 y volve a ejecutar este archivo.
    pause
    exit /b 1
)

if not exist "%~dp0target\apploans-1.0.0.jar" (
    echo No se encontro el archivo compilado.
    echo Primero ejecuta: mvn -DskipTests package
    pause
    exit /b 1
)

java -jar "%~dp0target\apploans-1.0.0.jar"
if errorlevel 1 pause
