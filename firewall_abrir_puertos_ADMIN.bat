@echo off
rem ===================================================================
rem  SOLO EN LA PC SERVIDOR. Clic derecho -> "Ejecutar como administrador".
rem  Abre los puertos TCP 1099 (registro RMI) y 1100 (objeto remoto).
rem  MySQL (3310) NO se abre: solo lo usa el servidor de forma local.
rem ===================================================================
net session >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Necesitas permisos de administrador.
  echo Clic derecho sobre este archivo -^> Ejecutar como administrador.
  pause & exit /b 1
)
netsh advfirewall firewall delete rule name="RMI Laboratorio (TCP 1099-1100)" >nul 2>&1
netsh advfirewall firewall add rule name="RMI Laboratorio (TCP 1099-1100)" dir=in action=allow protocol=TCP localport=1099-1100 profile=any
netsh advfirewall firewall show rule name="RMI Laboratorio (TCP 1099-1100)"
echo.
echo OJO: si alguna vez pulsaste "Cancelar" en el aviso de Windows Defender para Java,
echo existe una regla de BLOQUEO para java.exe que gana sobre esta. Revisala en wf.msc
echo (Reglas de entrada, busca "OpenJDK Platform binary" o "Java") y eliminala.
pause
