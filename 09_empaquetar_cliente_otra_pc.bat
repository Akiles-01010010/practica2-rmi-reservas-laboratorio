@echo off
rem Crea Paquete_Cliente.zip con TODO lo necesario para correr el cliente en OTRA computadora.
rem En la otra PC solo se necesita Java 17 o superior (no MySQL, no Docker).
setlocal
cd /d "%~dp0"
call "%~dp0config_red.bat"
if not exist Cliente\dist\cliente.jar (
  echo [ERROR] Primero ejecuta 05_compilar_todo.bat
  pause & exit /b 1
)
if exist Paquete_Cliente rmdir /s /q Paquete_Cliente
mkdir Paquete_Cliente\lib
copy /y Cliente\dist\cliente.jar Paquete_Cliente\ >nul
copy /y lib\comun.jar Paquete_Cliente\lib\ >nul
copy /y lib\flatlaf.jar Paquete_Cliente\lib\ >nul
copy /y Cliente\paquete\*.* Paquete_Cliente\ >nul
>Paquete_Cliente\servidor_ip.txt echo %SERVER_IP%
if exist Paquete_Cliente.zip del /q Paquete_Cliente.zip
%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -Command "Compress-Archive -Path 'Paquete_Cliente\*' -DestinationPath 'Paquete_Cliente.zip'"
echo [OK] Creado Paquete_Cliente.zip (IP del servidor dentro: %SERVER_IP%)
echo Copialo a la otra PC, descomprimelo y ejecuta ejecutar_cliente.bat
pause
endlocal
