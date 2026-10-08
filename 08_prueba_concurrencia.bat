@echo off
rem PRUEBA DE ESTRES: N hilos reservan el MISMO equipo a la vez. Solo uno debe ganar.
rem Uso: 08_prueba_concurrencia.bat [equipo] [hilos] [prefijo] [HH:mm:ss]
rem   ej. misma PC        : 08_prueba_concurrencia.bat 7 20 A
rem   ej. PC A y PC B     : 08_prueba_concurrencia.bat 9 10 PCA 18:30:00   (en la PC A)
rem                         prueba_concurrencia.bat    9 10 PCB 18:30:00   (en la PC B)
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
set EQUIPO=%~1
set HILOS=%~2
set PREFIJO=%~3
if "%EQUIPO%"=="" set EQUIPO=7
if "%HILOS%"=="" set HILOS=10
if "%PREFIJO%"=="" set PREFIJO=A
chcp 65001 >nul
cd Cliente
java -Dstdout.encoding=UTF-8 -cp "dist\cliente.jar;..\lib\comun.jar" cliente.PruebaConcurrencia %SERVER_IP% %EQUIPO% %HILOS% %PREFIJO% %~4
pause
endlocal
