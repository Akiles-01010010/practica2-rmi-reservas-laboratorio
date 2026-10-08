@echo off
rem Compila los 3 modulos en orden: Interfaces -> Servidor -> Cliente
setlocal
cd /d "%~dp0"
set NOPAUSE=1
java -version 2>&1 | findstr /i "version"
rem Si se clono el repositorio, las librerias de terceros aun no existen: se descargan
if not exist "%~dp0lib\mysql-connector-j.jar" call "%~dp0descargar_librerias.bat" || goto error
if not exist "%~dp0lib\flatlaf.jar" call "%~dp0descargar_librerias.bat" || goto error
call "%~dp002_compilar_interfaces.bat" || goto error
call "%~dp003_compilar_servidor.bat"   || goto error
call "%~dp004_compilar_cliente.bat"    || goto error
echo.
echo [OK] Todo compilado (bytecode Java 17: corre en JDK 17 o superior).
pause
endlocal & exit /b 0
:error
echo.
echo [ERROR] La compilacion se detuvo. Lee el mensaje de arriba.
pause
endlocal & exit /b 1
