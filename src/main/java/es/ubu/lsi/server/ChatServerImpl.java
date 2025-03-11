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

import es.ubu.lsi.common.ChatMessage;

public class ChatServerImpl implements ChatServer {

	private static final int DEFAULT_PORT = 1500;
	
	private static int clientId;
	
	private static SimpleDateFormat sdf;
	
	private int port;
	
	private boolean alive;
	
	private Map<Integer, ServerThreadForClient> clients;
	
	// --------------------------------------------------------------------------------
	
	public ChatServerImpl(int port) {
		
		this.port = port;
		clientId = 0;
		sdf = new SimpleDateFormat("HH:mm:ss");
		this.alive = true;
		this.clients = new HashMap<>();
	}
	
	public static void main(String[] args) {
		
		// No recibe argumentos en la invocación
	    ChatServerImpl server = new ChatServerImpl(DEFAULT_PORT);
	    server.startup();
	}
	
	// --------------------------------------------------------------------------------
	
	@Override
	public void startup() {
		
		try (ServerSocket serverSocket = new ServerSocket(port)) {
            
			System.out.println("<Servidor iniciado en el puerto " + port + ">");

            while (alive) {
                Socket socket = serverSocket.accept();
                clientId++; // Incrementamos el contador de clientes
                System.out.println(sdf.format(new Date()) + " - Nuevo cliente conectado: ID " + clientId);
                
                // Creamos un hilo para el cliente
                ServerThreadForClient thread = new ServerThreadForClient(clientId, socket);
                clients.put(clientId, thread); // Guardamos el cliente en el mapa
                thread.start();
            }
        } catch (IOException e) {
        	shutdown();
            System.out.println("ERROR en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
	}

	@Override
	public void shutdown() {
		System.out.println("<Apagando servidor...>");
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
            System.out.println("<Cliente " + id + " eliminado>");
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
                
                username = (String) input.readObject(); //TODO cambiar para que lea el username correcto
                
                System.out.println("Usuario " + username + " conectado con ID " + id);
            } 
            catch (IOException | ClassNotFoundException e) {
                System.out.println("ERROR con el cliente " + id);
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
                            System.out.println(sdf.format(new Date())+" - LOGOUT Cliente " + id);
                            remove(id);
                            break;
                        default:
                        	break;
                    }
                }
            } 
            catch (IOException | ClassNotFoundException e) {
                System.out.println("Cliente " + id + " desconectado inesperadamente.");
                e.printStackTrace();
            } 
            finally {
                closeConnection();
            }
        }
        
        public void sendMessage(ChatMessage message) {
            
        	try {
                output.writeObject(message);
            } 
            catch (IOException e) {
                System.out.println("ERROR enviando mensaje a " + username);
            }
        }

        public void closeConnection() {
            
        	try {
                if (socket != null) socket.close();
                if (input != null) input.close();
                if (output != null) output.close();
            } 
        	catch (IOException e) {
                System.out.println("ERROR cerrando conexión del cliente " + id);
            }
        }
		
	}

}
