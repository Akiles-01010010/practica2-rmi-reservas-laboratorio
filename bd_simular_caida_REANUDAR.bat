@echo off
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
docker unpause %MYSQL_CONTAINER%
echo [OK] MySQL responde otra vez.
pause
endlocal
