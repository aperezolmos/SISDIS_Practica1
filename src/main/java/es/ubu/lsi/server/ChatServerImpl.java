package es.ubu.lsi.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

import es.ubu.lsi.common.ChatMessage;

/**
 * Implementación del servidor de chat.
 * Acepta conexiones de clientes y retransmite mensajes.
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
	
	
	/**
	 * Mapa que almacena los IDs de los clientes conectados junto 
	 * a sus hilos correspondientes. 
	 */
	private Map<Integer, ServerThreadForClient> clients;
	
	// --------------------------------------------------------------------------------
	
	/** Logger. */
	private static final Logger logger = Logger.getLogger(ChatServerImpl.class.getName());

    static {
        try {
            FileHandler fileHandler = new FileHandler("chat.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } 
        catch (IOException e) {
            System.out.println("[ERROR - Logger] Fallo al configurar el logger: " + e.getMessage());
        }
    }
	
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
		this.clients = new HashMap<>();
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
		
		for (ServerThreadForClient client : clients.values()) {
			client.sendMessage(message);
		}
	}

	@Override
	public void remove(int id) {
		
		if (clients.containsKey(id)) {
            clients.remove(id);
            System.out.println("[INFO - Server] " + sdf.format(new Date()) + " - Cliente " + id + " desconectado");
        }
	}
	
	/**
	 * Maneja un mensaje para operar dependiendo de su contenido.
	 * 
	 * @param message Mensaje a manejar
	 */
	private void handleMessage(ChatMessage message) {
		
		if (message.getMessage().startsWith("[BAN]") || message.getMessage().startsWith("[UNBAN]")) {
			// Si es un mensaje de ban/unban, solo se muestra en el servidor
			String text = message.getMessage().split("]")[1].trim();
	        System.out.println("[BAN/UNBAN] " + sdf.format(new Date()) + " - " + text);
	    } 
		else {
			String wrappedMessage = "[MSG] " + sdf.format(new Date()) + 
					" - Amanda Pérez patrocina el mensaje -> " + message.getMessage();
			
			//logger.info(message.getMessage());
			broadcast(new ChatMessage(message.getId(), message.getType(), wrappedMessage));
	    }
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
                
                username = (String) input.readObject();
                
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
