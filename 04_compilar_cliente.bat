@echo off
rem Compila el modulo Cliente (GUI Swing + prueba de concurrencia). Necesita lib\comun.jar
setlocal
cd /d "%~dp0Cliente"
echo === [3/3] Compilando Cliente -^> cliente.jar ===
if not exist ..\lib\comun.jar (
  echo [ERROR] Falta lib\comun.jar: ejecuta primero 02_compilar_interfaces.bat
  goto error
)
if exist bin rmdir /s /q bin
mkdir bin
javac --release 17 -encoding UTF-8 -cp "..\lib\comun.jar" -d bin src\cliente\*.java
if errorlevel 1 goto error
if not exist dist mkdir dist
jar --create --file dist\cliente.jar --main-class cliente.ClienteMain -C bin .
if errorlevel 1 goto error
echo [OK] Cliente\dist\cliente.jar generado
if not defined NOPAUSE pause
endlocal & exit /b 0
:error
echo [ERROR] Fallo la compilacion del Cliente.
if not defined NOPAUSE pause
endlocal & exit /b 1
