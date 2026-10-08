@echo off
rem Compila el modulo Servidor (necesita lib\comun.jar). El driver MySQL solo se usa al EJECUTAR.
setlocal
cd /d "%~dp0Servidor"
echo === [2/3] Compilando Servidor -^> servidor.jar ===
if not exist ..\lib\comun.jar (
  echo [ERROR] Falta lib\comun.jar: ejecuta primero 02_compilar_interfaces.bat
  goto error
)
if exist bin rmdir /s /q bin
mkdir bin
javac --release 17 -encoding UTF-8 -cp "..\lib\comun.jar" -d bin src\servidor\*.java src\servidor\bd\*.java src\servidor\dao\*.java
if errorlevel 1 goto error
if not exist dist mkdir dist
jar --create --file dist\servidor.jar --main-class servidor.ServidorRMI -C bin .
if errorlevel 1 goto error
echo [OK] Servidor\dist\servidor.jar generado
if not defined NOPAUSE pause
endlocal & exit /b 0
:error
echo [ERROR] Fallo la compilacion del Servidor.
if not defined NOPAUSE pause
endlocal & exit /b 1
