# Práctica 1 - Chat 1.0

Sistema de chat distribuido en Java utilizando sockets TCP.

Amanda Pérez Olmos [apo1004@alu.ubu.es]

---

- La documentación se encuentra disponible en la carpeta `doc/` y ha sido generada mediante el fichero `build.xml` para su ejecución mediante ANT (`ant javadoc`).


## Ejecución del servidor y clientes con Maven

Antes de ejecutar el servidor o los clientes, es necesario compilar el proyecto:

```shell
mvn clean install
```
Se usa `exec-maven-plugin` para facilitar la ejecución. Los comandos disponibles son los siguientes:

```shell
# Iniciar el servidor en el puerto 1500
mvn exec:java@server

# Ejecutar un cliente con el nombre 'amanda'
mvn exec:java@client-amanda

# Ejecutar un cliente con el nombre 'perez'
mvn exec:java@client-perez
```

Para ejecutar clientes con distintos parámetros, se usa:

```shell
 mvn exec:java -Dexec.mainClass="es.ubu.lsi.client.ChatClientImpl" -Dexec.args="[servidor] <username>"
```
Donde el nombre de usuario será obligatorio especificarlo, mientras que el del servidor se establece por defecto a `localhost` si no se pasan 2 argumentos. El servidor `ChatServerImpl` no recibe argumentos en su invocación.