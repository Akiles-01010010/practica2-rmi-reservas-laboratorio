@echo off
rem Compila el modulo comun (interfaz remota + DTOs + excepciones) y genera comun.jar
setlocal
cd /d "%~dp0Interfaces"
echo === [1/3] Compilando Interfaces -^> comun.jar ===
if exist bin rmdir /s /q bin
mkdir bin
javac --release 17 -encoding UTF-8 -d bin src\comun\*.java
if errorlevel 1 goto error
if not exist dist mkdir dist
jar --create --file dist\comun.jar -C bin .
if errorlevel 1 goto error
copy /y dist\comun.jar ..\lib\comun.jar >nul
echo [OK] Interfaces\dist\comun.jar generado y copiado a lib\comun.jar
if not defined NOPAUSE pause
endlocal & exit /b 0
:error
echo [ERROR] Fallo la compilacion de Interfaces.
if not defined NOPAUSE pause
endlocal & exit /b 1
