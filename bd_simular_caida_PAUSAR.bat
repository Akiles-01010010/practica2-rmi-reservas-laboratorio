@echo off
rem Congela el contenedor MySQL: la BD "deja de responder" (para probar los TIMEOUTS).
rem Para regresarla a la normalidad usa bd_simular_caida_REANUDAR.bat
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
docker pause %MYSQL_CONTAINER%
echo [OK] MySQL CONGELADO. Intenta reservar desde un cliente: debe salir "Tiempo de espera agotado".
pause
endlocal
