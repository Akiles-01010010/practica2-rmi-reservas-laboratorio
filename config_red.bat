@echo off
rem =====================================================================
rem  CONFIGURACION DE RED - la usan todos los demas .bat
rem
rem  OPCION B: misma red local (Wi-Fi o cable), SIN Tailscale.
rem
rem  SERVER_IP = IP de la computadora que corre el SERVIDOR RMI (y MySQL).
rem    AUTO  -> se detecta sola: la IPv4 de la tarjeta que tiene salida a
rem             internet (Wi-Fi/Ethernet), ignorando Tailscale, WSL,
rem             VirtualBox y VMware. Recomendado, porque el router puede
rem             cambiar la IP cada vez que te conectas (DHCP).
rem    o una IP fija, por ejemplo:  set SERVER_IP=192.168.3.142
rem
rem  IMPORTANTE: servidor y TODOS los clientes deben usar la MISMA IP y
rem  estar conectados al MISMO router / red.
rem =====================================================================
set SERVER_IP=AUTO
set RMI_PORT=1099
set RMI_OBJ_PORT=1100
set MYSQL_CONTAINER=mysql-reservas-lab
set MYSQL_ROOT_PASS=RootLab2026

if /i not "%SERVER_IP%"=="AUTO" goto :eof
set SERVER_IP=
for /f "usebackq delims=" %%i in (`%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0detectar_ip.ps1"`) do set SERVER_IP=%%i
if "%SERVER_IP%"=="" (
  echo [AVISO] No se pudo detectar la IP de la red local. Se usa 127.0.0.1
  echo         ^(solo funcionara en esta PC^). Revisa tu conexion Wi-Fi/cable.
  set SERVER_IP=127.0.0.1
)
