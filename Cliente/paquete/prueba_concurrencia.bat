@echo off
rem PRUEBA DE ESTRES desde otra computadora.
rem Uso: prueba_concurrencia.bat [equipo] [hilos] [prefijo] [HH:mm:ss]
rem   ej: prueba_concurrencia.bat 9 10 PCB 18:30:00
setlocal
cd /d "%~dp0"
set /p SERVER_IP=<servidor_ip.txt
set EQUIPO=%~1
set HILOS=%~2
set PREFIJO=%~3
if "%EQUIPO%"=="" set EQUIPO=7
if "%HILOS%"=="" set HILOS=10
if "%PREFIJO%"=="" set PREFIJO=B
chcp 65001 >nul
java -Dstdout.encoding=UTF-8 -cp "cliente.jar;lib\comun.jar" cliente.PruebaConcurrencia %SERVER_IP% %EQUIPO% %HILOS% %PREFIJO% %~4
pause
endlocal
