package es.ubu.lsi.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Implementación del servidor de chat.
 * Acepta conexiones de clientes y retransmite mensajes.
 * También gestiona las listas de usuarios baneados por cada cliente.
 * 
 * @author Amanda Pérez Olmos
 */
public class ChatServerImpl implements ChatServer {

	/** Puerto por defecto del servidor. */
	private static final int DEFAULT_PORT = 1500;
	
	
	/** Contador de clientes. */
	private static int clientId;
	
	/** Formato de fecha. */
	private static SimpleDateFormat sdf;
	
	/** Puerto del servidor. */
	private int port;
	
	/** Estado del servidor. */
	private boolean alive;
	
	
	/** Almacena los IDs de los clientes conectados junto a sus hilos correspondientes. */
	private Map<Integer, ServerThreadForClient> clients = new HashMap<>();
	
	/** Almacena la relación 'ID cliente - nombre de usuario'. */
	private Map<Integer, String> clientUsernames = new HashMap<>();
	
	/** Almacena la relación 'nombre de usuario - lista de usuarios baneados'. */
	private Map<String, Set<String>> banListPerClient = new HashMap<>();
	
	// --------------------------------------------------------------------------------
	
    /**
     * Inicializa un servidor.
     * 
     * @param port Puerto de escucha
     */
	public ChatServerImpl(int port) {
		
		this.port = port;
		clientId = 0;
		sdf = new SimpleDateFormat("HH:mm:ss");
		this.alive = true;
	}
	
	/**
	 * Método principal para ejecutar el servidor.
	 * 
	 * @param args Argumentos de línea de comandos (no se utilizan)
	 */
	public static void main(String[] args) {
		
	    ChatServerImpl server = new ChatServerImpl(DEFAULT_PORT);
	    server.startup();
	}
	
	// --------------------------------------------------------------------------------
	
	@Override
	public void startup() {
		
		try (ServerSocket serverSocket = new ServerSocket(port)) {
            
			System.out.println("[INFO - Server] " + sdf.format(new Date()) + 
								" - Servidor iniciado en el puerto " + port);

            while (alive) {
                Socket socket = serverSocket.accept();
                clientId++; // Incrementamos el contador de clientes
                
                // Creamos un hilo para el cliente
                ServerThreadForClient thread = new ServerThreadForClient(clientId, socket);
                clients.put(clientId, thread);
                thread.start();
            }
        } 
		catch (IOException e) {
			shutdown();
			System.out.println("[ERROR - Server] " + e.getMessage());
			e.printStackTrace();
        }
	}

	@Override
	public void shutdown() {
		
		System.out.println("[INFO - Server] " + sdf.format(new Date()) + " - Apagando servidor...");
        alive = false;

        for (ServerThreadForClient client : clients.values()) {
            client.closeConnection();
        }
        clients.clear();
	}

	@Override
	public void broadcast(ChatMessage message) {
		
		String senderUsername = clientUsernames.get(message.getId());
	    
	    for (ServerThreadForClient client : clients.values()) {
	        String recipientUsername = clientUsernames.get(client.id);

	        // Verificamos si el remitente está en la lista de baneados del destinatario
	        Set<String> bannedUsers = banListPerClient.get(recipientUsername);
	        
	        if (bannedUsers == null || !bannedUsers.contains(senderUsername)) {
	            client.sendMessage(message);
	        }
	    }
	}

	@Override
	public void remove(int id) {
		
		if (clients.containsKey(id)) {
            clients.remove(id);
            System.out.println("[INFO - Server] " + sdf.format(new Date()) + " - Cliente " +
            					clientUsernames.get(id) + " (ID="+ id + ") desconectado");
        }
	}
	
	/**
	 * Maneja un mensaje para operar dependiendo de su contenido.
	 * 
	 * @param message Mensaje a manejar
	 */
	private void handleMessage(ChatMessage message) {
		
		if (message.getMessage().startsWith("[BAN]")) {
			String text = message.getMessage().split("]")[1].trim();
			handleBan(text);
			System.out.println("[BAN] " + sdf.format(new Date()) + " - " + text);
	    } 
		else if (message.getMessage().startsWith("[UNBAN]")) {
			String text = message.getMessage().split("]")[1].trim();
			handleUnban(text);
			System.out.println("[UNBAN] " + sdf.format(new Date()) + " - " + text);
		}
		else {
			String wrappedMessage = "[MSG] " + sdf.format(new Date()) + 
					" - Amanda Perez patrocina el mensaje -> " + message.getMessage();
			
			broadcast(new ChatMessage(message.getId(), message.getType(), wrappedMessage));
	    }
	}
	
	/**
	 * Maneja un mensaje de BAN y añade un usuario a la lista de usuarios 
	 * baneados del cliente.
	 * 
	 * @param text Texto del mensaje
	 */
	private void handleBan(String text) {
		
		String username = text.split("ha baneado a")[0].trim();
		String userBanned = text.split("ha baneado a")[1].trim();
		banListPerClient.computeIfAbsent(username, k -> new HashSet<>()).add(userBanned);
	}
	
	/**
	 * Maneja un mensaje de UNBAN y elimina un usuario de la lista de usuarios 
	 * baneados del cliente.
	 * 
	 * @param text Texto del mensaje
	 */
	private void handleUnban(String text) {
		
		String username = text.split("ha desbaneado a")[0].trim();
		String userBanned = text.split("ha desbaneado a")[1].trim();
		banListPerClient.getOrDefault(username, new HashSet<>()).remove(userBanned);
	}
	
	// --------------------------------------------------------------------------------
	
	/**
	 * Hilo independiente de cada cliente asociado en el servidor.
	 */
	public class ServerThreadForClient extends Thread {
		
		/** Id del usuario. */
		private int id;
		
		/** Nickname del usuario. */
		private String username;
		
		
		/** Socket para la conexión con el cliente. */
		private Socket socket;
        
		/** Flujo de entrada para recibir mensajes de los clientes. */
        private ObjectInputStream input;

        /** Flujo de salida para enviar mensajes a los clientes. */
        private ObjectOutputStream output;
        
        
        /**
         * Construye un ServerThreadForClient para el cliente especificado.
         * 
         * @param id ID del cliente
         * @param socket Socket para la conexión con el cliente
         */
        public ServerThreadForClient(int id, Socket socket) {
        	
            this.id = id;
            this.socket = socket;
            
            try {
            	output = new ObjectOutputStream(socket.getOutputStream());
            	input = new ObjectInputStream(socket.getInputStream());
                
            	// El cliente envía su nombre de usuario al servidor y el servidor le manda su ID asociado
                username = (String) input.readObject();
                clientUsernames.put(id, username);
                sendMessage(new ChatMessage(id, MessageType.MESSAGE, "Registro correcto"));
                
                System.out.println("[INFO - Server] " + sdf.format(new Date()) + " - Nuevo usuario \'" 
                					+ username + "\' conectado (ID=" + clientId + ")");
            } 
            catch (IOException | ClassNotFoundException e) {
                System.out.println("[ERROR - Server] Fallo con el cliente " + id + ": " + e.getMessage());
            }
        }
        
        /**
         * Espera en un bucle a los mensajes recibidos de cada cliente (flujo de entrada) 
         * para realizar la operaciones correspondientes.
         */
        @Override
        public void run() {
        	
        	boolean running = true;
        	
            try {
            	while (running) {
            		ChatMessage message = (ChatMessage) input.readObject();
                    
            		switch (message.getType()) {
                        case MESSAGE:
                            handleMessage(message);
                            break;
                        case LOGOUT:
                            running = false;
                            remove(id);
                            break;
                        default:
                        	break;
                    }
                }
            } 
            catch (IOException | ClassNotFoundException e) {
                System.out.println("[ERROR - Server] Cliente " + id + " desconectado inesperadamente.");
            } 
            finally {
                closeConnection();
            }
        }
        
        /**
         * Envía un mensaje a través del flujo de salida.
         * 
         * @param message Mensaje a enviar
         */
        private void sendMessage(ChatMessage message) {
            
        	try {
                output.writeObject(message);
            } 
            catch (IOException e) {
                System.out.println("[ERROR - Server] Fallo al enviar mensaje a " + username);
            }
        }

        /**
         * Libera los recursos.
         */
        private void closeConnection() {
            
        	try {
                if (socket != null) socket.close();
                if (input != null) input.close();
                if (output != null) output.close();
            } 
        	catch (IOException e) {
                System.out.println("[ERROR - Server] Fallo al cerrar conexión del cliente " + id);
            }
        }
		
	}

}
