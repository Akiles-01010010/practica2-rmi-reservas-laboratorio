@echo off
rem Crea (la primera vez) o inicia el contenedor MySQL exclusivo de esta practica.
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"

docker info >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker Desktop no esta encendido. Abrelo y espera a "Engine running".
  goto error
)

docker ps -a --format "{{.Names}}" | findstr /x "%MYSQL_CONTAINER%" >nul
if errorlevel 1 (
  echo Creando contenedor %MYSQL_CONTAINER% (MySQL 8.4, puerto 127.0.0.1:3310^)...
  docker run -d --name %MYSQL_CONTAINER% -e MYSQL_ROOT_PASSWORD=%MYSQL_ROOT_PASS% -e MYSQL_DATABASE=laboratorio_rmi -e MYSQL_USER=rmi_user -e MYSQL_PASSWORD=RmiLab2026 -e TZ=America/Mexico_City -p 127.0.0.1:3310:3306 -v mysql_reservas_lab_data:/var/lib/mysql --restart unless-stopped mysql:8.4
  if errorlevel 1 goto error
) else (
  echo Iniciando contenedor existente %MYSQL_CONTAINER%...
  docker start %MYSQL_CONTAINER% >nul
)

echo Esperando a que MySQL acepte conexiones TCP...
set /a intentos=0
:esperar
docker exec %MYSQL_CONTAINER% mysql -h127.0.0.1 -urmi_user -pRmiLab2026 -e "SELECT 1" >nul 2>&1
if not errorlevel 1 goto listo
set /a intentos+=1
if %intentos% geq 60 (
  echo [ERROR] MySQL no respondio en 2 minutos. Revisa: docker logs %MYSQL_CONTAINER%
  goto error
)
timeout /t 2 /nobreak >nul
goto esperar

:listo
echo [OK] MySQL listo en 127.0.0.1:3310 (base laboratorio_rmi, usuario rmi_user)
docker ps --filter name=%MYSQL_CONTAINER% --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
if not defined NOPAUSE pause
endlocal & exit /b 0
:error
if not defined NOPAUSE pause
endlocal & exit /b 1
