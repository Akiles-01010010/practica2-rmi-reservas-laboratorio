@echo off
rem CLIENTE en otra computadora (Opcion B: misma red Wi-Fi/cable que el servidor).
rem Uso: ejecutar_cliente.bat [IP_SERVIDOR]
rem Si no pasas IP, te pregunta usando como sugerencia la de servidor_ip.txt
setlocal
cd /d "%~dp0"
set /p SERVER_IP=<servidor_ip.txt
if not "%~1"=="" (
  set "SERVER_IP=%~1"
  goto ejecutar
)
echo La IP del servidor aparece en su ventana: "IP anunciada a clientes".
set "NUEVA="
set /p "NUEVA=IP del servidor [%SERVER_IP%] (Enter para usar esa): "
if not "%NUEVA%"=="" (
  set "SERVER_IP=%NUEVA%"
  >servidor_ip.txt echo %NUEVA%
)
:ejecutar
chcp 65001 >nul
title CLIENTE RMI  -^>  %SERVER_IP%:1099
java -version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Esta PC no tiene Java. Instala JDK/JRE 17 o superior.
  pause & exit /b 1
)
echo Conectando al servidor %SERVER_IP%:1099 ...
java -Dstdout.encoding=UTF-8 -cp "cliente.jar;lib\comun.jar;lib\flatlaf.jar" cliente.ClienteMain %SERVER_IP% 1099
endlocal
