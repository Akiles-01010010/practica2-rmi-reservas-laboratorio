# Devuelve la IPv4 de la red local real (la tarjeta con puerta de enlace: Wi-Fi o Ethernet),
# ignorando adaptadores virtuales (Tailscale, WSL/Hyper-V, VMware, VirtualBox).
$cfg = Get-NetIPConfiguration |
    Where-Object { $_.IPv4DefaultGateway -ne $null -and $_.NetAdapter.Status -eq 'Up' -and
                   $_.InterfaceAlias -notmatch 'Tailscale|vEthernet|VMware|VirtualBox|Loopback' } |
    Sort-Object { $_.IPv4DefaultGateway.RouteMetric + $_.NetIPv4Interface.InterfaceMetric } |
    Select-Object -First 1
if ($cfg) { $cfg.IPv4Address | Select-Object -First 1 -ExpandProperty IPAddress }
