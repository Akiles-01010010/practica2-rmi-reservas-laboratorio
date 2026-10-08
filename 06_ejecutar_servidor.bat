@echo off
rem Ejecuta el SERVIDOR RMI. Uso: 06_ejecutar_servidor.bat [IP]   (por defecto SERVER_IP de config_red.bat)
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
if not "%~1"=="" set SERVER_IP=%~1
chcp 65001 >nul
title SERVIDOR RMI  %SERVER_IP%:%RMI_PORT%  (objeto en %RMI_OBJ_PORT%)
if not exist Servidor\dist\servidor.jar (
  echo [ERROR] No existe Servidor\dist\servidor.jar: ejecuta 05_compilar_todo.bat
  pause & exit /b 1
)
cd Servidor
rem -Djava.rmi.server.hostname : IP que viaja dentro del stub (la que usaran los clientes)
java -Dstdout.encoding=UTF-8 -Djava.rmi.server.hostname=%SERVER_IP% -cp "dist\servidor.jar;..\lib\comun.jar;..\lib\mysql-connector-j.jar" servidor.ServidorRMI servidor.properties
pause
endlocal
