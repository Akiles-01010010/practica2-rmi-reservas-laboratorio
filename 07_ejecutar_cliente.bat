@echo off
rem Ejecuta un CLIENTE (GUI). Uso: 07_ejecutar_cliente.bat [IP_SERVIDOR]
rem Puedes abrirlo varias veces para tener varios clientes en la misma PC.
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
if not "%~1"=="" set SERVER_IP=%~1
chcp 65001 >nul
title CLIENTE RMI  -^>  %SERVER_IP%:%RMI_PORT%
if not exist Cliente\dist\cliente.jar (
  echo [ERROR] No existe Cliente\dist\cliente.jar: ejecuta 05_compilar_todo.bat
  pause & exit /b 1
)
cd Cliente
java -Dstdout.encoding=UTF-8 -cp "dist\cliente.jar;..\lib\comun.jar;..\lib\flatlaf.jar" cliente.ClienteMain %SERVER_IP% %RMI_PORT%
endlocal
