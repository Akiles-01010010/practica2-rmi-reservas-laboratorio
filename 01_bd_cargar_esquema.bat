@echo off
rem Carga BD\01_esquema_y_datos.sql (BORRA y recrea tablas con 20 equipos).
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
echo ATENCION: esto BORRA las tablas equipos/reservas/bitacora y vuelve a cargarlas.
choice /c SN /m "Continuar"
if errorlevel 2 goto fin
docker exec -i %MYSQL_CONTAINER% mysql -uroot -p%MYSQL_ROOT_PASS% --default-character-set=utf8mb4 < BD\01_esquema_y_datos.sql
if errorlevel 1 (
  echo [ERROR] No se pudo cargar el esquema. Esta encendido el contenedor? Ejecuta 00_mysql_crear_o_iniciar.bat
) else (
  echo [OK] Esquema cargado.
)
:fin
if not defined NOPAUSE pause
endlocal
