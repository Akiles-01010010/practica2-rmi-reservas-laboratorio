@echo off
rem Cancela TODAS las reservas activas (los equipos vuelven a DISPONIBLE). Conserva la bitacora.
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
docker exec -i %MYSQL_CONTAINER% mysql -uroot -p%MYSQL_ROOT_PASS% --default-character-set=utf8mb4 -t < BD\02_reiniciar_reservas.sql 2>nul
if errorlevel 1 (echo [ERROR] No se pudo reiniciar.) else (echo [OK] Reservas reiniciadas.)
pause
endlocal
