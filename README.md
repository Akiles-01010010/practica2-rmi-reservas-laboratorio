# Sistema Distribuido de Reservas e Inventario de Laboratorios (Java RMI)

**Tema 3 RMI – Práctica 2** · Programación en ambiente cliente servidor
Tecnológico Nacional de México, Campus Iguala · Ingeniería Informática · 7.º semestre, grupo U

Aplicación distribuida en **3 capas** que permite a varios alumnos, desde distintas computadoras, reservar
equipos de un laboratorio de 20 computadoras sin que dos personas obtengan el mismo equipo:

| Capa | Tecnología | Módulo |
|---|---|---|
| 1. Presentación | Swing (+ FlatLaf opcional): plano 4×5 con colores por estado | `Cliente/` |
| 2. Lógica y control concurrente | Objeto remoto Java RMI, `ReentrantLock` por equipo con `tryLock` (timeout) | `Servidor/` |
| 3. Persistencia | DAO + JDBC + MySQL 8.4 en Docker, transacciones `commit`/`rollback` | `Servidor/` + `BD/` |

El módulo `Interfaces/` (→ `comun.jar`) contiene la interfaz remota, los DTO `Serializable`
(`ReservaRequest`, `TicketReserva`, `EquipoDTO`) y las excepciones de negocio.

## Estados de un equipo

| Color | Estado | Significado |
|---|---|---|
| 🟩 Verde | `DISPONIBLE` | Se puede reservar |
| 🟨 Amarillo | `EN_PROCESO` | Otro alumno lo está reservando (bloqueo temporal de 60 s en memoria) |
| 🟥 Rojo | `RESERVADO` | Tiene un ticket confirmado en la BD |
| ⬜ Gris | `MANTENIMIENTO` | No disponible |

## Requisitos

- **PC servidor:** Windows, JDK 17 o superior, Docker Desktop.
- **PC cliente:** solo Java 17 o superior.
- Todas las PCs en la **misma red local** (mismo Wi-Fi o router).

## Puesta en marcha (PC servidor)

```bat
descargar_librerias.bat          :: Connector/J y FlatLaf desde Maven Central
00_mysql_crear_o_iniciar.bat     :: contenedor mysql-reservas-lab (127.0.0.1:3310)
01_bd_cargar_esquema.bat         :: tablas + 20 equipos (solo la primera vez)
05_compilar_todo.bat             :: comun.jar, servidor.jar, cliente.jar (javac --release 17)
firewall_abrir_puertos_ADMIN.bat :: como administrador, una vez (TCP 1099-1100)
06_ejecutar_servidor.bat         :: muestra "IP anunciada a clientes"
07_ejecutar_cliente.bat          :: cliente en la misma PC
```

La IP del servidor se **detecta automáticamente** (`config_red.bat` usa `SERVER_IP=AUTO` y `detectar_ip.ps1`).

## Cliente en otra PC

1. En el servidor: `09_empaquetar_cliente_otra_pc.bat` → genera `Paquete_Cliente.zip`.
2. En la otra PC: descomprimir y ejecutar `ejecutar_cliente.bat` (pide la IP del servidor).
3. Comprobar la red: `Test-NetConnection <IP_SERVIDOR> -Port 1099` debe dar `True`.

## Pruebas

| Prueba | Cómo |
|---|---|
| Concurrencia | `08_prueba_concurrencia.bat 7 20 A` → 20 hilos, un solo ganador |
| Desconexión de red | Desconectar al cliente con el equipo EN_PROCESO o con `reserva.demoraTransaccionMs` > 0; revisar `bd_diagnostico.bat` |
| BD sin respuesta (timeout) | `bd_simular_caida_PAUSAR.bat` / `bd_simular_caida_REANUDAR.bat` |
| Persistencia | Reservar, cerrar y volver a abrir el servidor: lee el estado desde MySQL |

La guía completa está en **`Instructivo_Practica2_RMI.docx`** y el reporte en **`Reporte/Reporte_Practica2_RMI.docx`**.

## Estructura

```
BD/            scripts SQL (esquema, reinicio, diagnóstico)
Interfaces/    módulo común (interfaz remota, DTO, excepciones)
Servidor/      objeto remoto, DAO, transacciones, servidor.properties
Cliente/       GUI Swing, prueba de concurrencia, plantillas para la otra PC
Reporte/       reporte de prácticas e imágenes
*.bat          compilar, ejecutar, red, firewall y utilidades de BD
```

> Las contraseñas de `servidor.properties` y de los scripts son solo para el contenedor local de prueba
> (MySQL publicado únicamente en `127.0.0.1`). No reutilizarlas en otros sistemas.

## Autores

Yexson Salgado Sandoval · Aaron Alexander Neri Barrios · Marco Aurelio Juarez Baltazar · Cesar Said Corona Naneja

Profesora: María del Carmen Urióstegui Peralta
