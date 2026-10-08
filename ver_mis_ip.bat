@echo off
rem Muestra las IPv4 de esta PC para elegir SERVER_IP en config_red.bat
echo === IPv4 de esta computadora ===
%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -Command "Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '169.254*' -and $_.IPAddress -ne '127.0.0.1' } | Format-Table InterfaceAlias,IPAddress -AutoSize"
where tailscale >nul 2>&1 && (echo === Tailscale === & tailscale ip -4)
echo Usa la de "Wi-Fi"/"Ethernet" (misma red) o la de "Tailscale" (100.x.x.x).
pause
