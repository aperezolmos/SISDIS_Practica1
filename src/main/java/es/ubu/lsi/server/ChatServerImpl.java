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

public class ChatServerImpl implements ChatServer {

	private static final int DEFAULT_PORT = 1500;
	
	
	private static int clientId;
	
	private static SimpleDateFormat sdf;
	
	private int port;
	
	private boolean alive;
	
	
	private Map<Integer, ServerThreadForClient> clients;
	
	// --------------------------------------------------------------------------------
	
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
	
	public ChatServerImpl(int port) {
		
		this.port = port;
		clientId = 0;
		sdf = new SimpleDateFormat("HH:mm:ss");
		this.alive = true;
		this.clients = new HashMap<>();
	}
	
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
		
		if (message.getMessage().startsWith("[BAN]") || message.getMessage().startsWith("[UNBAN]")) {
			// Si es un mensaje de ban/unban, solo se muestra en el servidor
			String text = message.getMessage().split("]")[1].trim();
	        System.out.println("[BAN/UNBAN] " + sdf.format(new Date()) + " - " + text);
	    } 
		else {
			String wrappedMessage = "[MSG] " + sdf.format(new Date()) + 
					" - Amanda Pérez patrocina el mensaje -> " + message.getMessage();
			
			//logger.info(message.getMessage());
			
			for (ServerThreadForClient client : clients.values()) {
				client.sendMessage(new ChatMessage(message.getId(), message.getType(), wrappedMessage));
			}
	    }
	}

	@Override
	public void remove(int id) {
		
		if (clients.containsKey(id)) {
            clients.remove(id);
            System.out.println("[INFO - Server] " + sdf.format(new Date()) + " - Cliente " + id + " desconectado");
        }
	}
	
	// --------------------------------------------------------------------------------
	
	public class ServerThreadForClient extends Thread {
		
		private int id;
		
		private String username;
		
		
		private Socket socket;
        
        private ObjectInputStream input;

        private ObjectOutputStream output;
        
        
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
        
        
        @Override
        public void run() {
        	
        	boolean running = true;
        	
            try {
            	while (running) {
            		ChatMessage message = (ChatMessage) input.readObject();
                    
            		switch (message.getType()) {
                        case MESSAGE:
                            broadcast(message);
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
        
        private void sendMessage(ChatMessage message) {
            
        	try {
                output.writeObject(message);
            } 
            catch (IOException e) {
                System.out.println("[ERROR - Server] Fallo al enviar mensaje a " + username);
            }
        }

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
