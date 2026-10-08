@echo off
rem Descarga las librerias de terceros a lib\ desde Maven Central (repositorio oficial).
rem   - MySQL Connector/J 26.7.0 (driver JDBC, lo usa el Servidor)
rem   - FlatLaf 3.7.2 (apariencia de la GUI, opcional, lo usa el Cliente)
rem No se suben al repositorio git: se descargan con este script.
setlocal
cd /d "%~dp0"
if not exist lib mkdir lib
set PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe
if not exist lib\mysql-connector-j.jar (
  echo Descargando mysql-connector-j 26.7.0...
  %PS% -NoProfile -Command "Invoke-WebRequest -UseBasicParsing https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/26.7.0/mysql-connector-j-26.7.0.jar -OutFile lib\mysql-connector-j.jar"
  if errorlevel 1 goto error
) else (
  echo lib\mysql-connector-j.jar ya existe.
)
if not exist lib\flatlaf.jar (
  echo Descargando FlatLaf 3.7.2...
  %PS% -NoProfile -Command "Invoke-WebRequest -UseBasicParsing https://repo1.maven.org/maven2/com/formdev/flatlaf/3.7.2/flatlaf-3.7.2.jar -OutFile lib\flatlaf.jar"
  if errorlevel 1 goto error
) else (
  echo lib\flatlaf.jar ya existe.
)
echo [OK] Librerias listas en lib\
if not defined NOPAUSE pause
endlocal & exit /b 0
:error
echo [ERROR] No se pudo descargar. Revisa tu conexion a internet.
if not defined NOPAUSE pause
endlocal & exit /b 1
