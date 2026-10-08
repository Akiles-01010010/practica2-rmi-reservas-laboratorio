@echo off
rem Muestra transacciones abiertas, filas bloqueadas, inventario y bitacora (pruebas 2 y 3).
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
chcp 65001 >nul
docker exec -i %MYSQL_CONTAINER% mysql -uroot -p%MYSQL_ROOT_PASS% --default-character-set=utf8mb4 -t < BD\03_diagnostico.sql 2>nul
if errorlevel 1 echo [ERROR] No se pudo consultar MySQL (esta encendido el contenedor?)
pause
endlocal
