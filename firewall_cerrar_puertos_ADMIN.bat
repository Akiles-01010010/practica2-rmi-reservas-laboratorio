@echo off
rem Quita la regla creada por firewall_abrir_puertos_ADMIN.bat (ejecutar como administrador).
net session >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Ejecuta como administrador.
  pause & exit /b 1
)
netsh advfirewall firewall delete rule name="RMI Laboratorio (TCP 1099-1100)"
pause
