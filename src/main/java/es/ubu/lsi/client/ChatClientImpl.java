package es.ubu.lsi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

public class ChatClientImpl implements ChatClient {

	private static final int DEFAULT_PORT = 1500;
	
	private static final String DEFAULT_SERVER = "localhost";
	
	
	private String server;
	
	private String username;
	
	private int port;
	
	private boolean carryOn = true;
	
	private int id;
	
	
	private Socket socket;
	
	private ObjectInputStream input;
	
    private ObjectOutputStream output;
    
    
    private Set<String> bannedUsers;
    
    // --------------------------------------------------------------------------------
	
	public ChatClientImpl(String server, String username, int port) {
		
		this.server = server;
		this.username = username;
		this.port = port;
		this.id = username.hashCode();
		this.bannedUsers = new HashSet<>();
	}
	
	public static void main(String[] args) {
		
		String server = DEFAULT_SERVER;
        String username = "";
        int port = DEFAULT_PORT;
        
        switch (args.length){
        	case 1:
        		username = args[0];
        		break;
        	case 2:
        		server = args[0];
        		username = args[1];
        		break;
        	default:
        		System.out.println("Uso: java es.ubu.lsi.client.ChatClientImpl [server] <nickname>");
        		break;
        }

        ChatClientImpl client = new ChatClientImpl(server, username, port);
        client.start();
	}
	
	// --------------------------------------------------------------------------------
	
	@Override
	public boolean start() {
		
		try {
            socket = new Socket(server, port);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());

            output.writeObject(username); // Enviar el nombre de usuario al servidor
            System.out.println("[WELCOME] Conectado al servidor como: " + username);

            new Thread(new ChatClientListener()).start(); // Hilo para escuchar mensajes

            handleInput();

            disconnect();
            return true;

        } 
		catch (IOException e) {
            System.out.println("[ERROR - Client] Fallo al conectar con el servidor -> " + e.getMessage());
            return false;
        }
	}

	@Override
	public void sendMessage(ChatMessage msg) {
		
		try {
            output.writeObject(msg);
        } 
		catch (IOException e) {
            System.out.println("[ERROR - Client] Fallo al enviar mensaje -> " + e.getMessage());
        }
	}

	@Override
	public void disconnect() {
		
		carryOn = false;
		try {
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();

            System.out.println("[EXIT] Desconectado del servidor");
        } 
		catch (IOException e) {
            System.out.println("[ERROR - Client] Fallo al cerrar la conexión -> " + e.getMessage());
        }
	}
	
	private void handleInput() {
		
		Scanner scanner = new Scanner(System.in);
        
        while (carryOn) {
            String message = scanner.nextLine();
            
            if (message.equalsIgnoreCase("logout")) {
                sendMessage(new ChatMessage(id, ChatMessage.MessageType.LOGOUT, ""));
                carryOn = false;
            }
            else if (message.startsWith("ban ")) {
                String bannedUser = message.substring(4).trim();
                bannedUsers.add(bannedUser);
                sendMessage(new ChatMessage(id, MessageType.MESSAGE, "[BAN] " + username + " ha baneado a " + bannedUser));
            } 
            else if (message.startsWith("unban ")) {
            	String unbannedUser = message.substring(6).trim();
            	bannedUsers.remove(unbannedUser);
            	sendMessage(new ChatMessage(id, MessageType.MESSAGE, "[UNBAN] " + username + " ha desbloqueado a " + unbannedUser));
            }
            else {
                String signedMessage = "@" + username + ": " + message;
                sendMessage(new ChatMessage(id, MessageType.MESSAGE, signedMessage));
            }
        }
        scanner.close();
	}
	
	// --------------------------------------------------------------------------------
	
	public class ChatClientListener implements Runnable {

		@Override
		public void run() {
			
			try {
				while (carryOn) {
	                ChatMessage msg = (ChatMessage) input.readObject();
	                String sender = (msg.getMessage().split("@")[1]).split(":")[0].trim();
	                
	                if (!bannedUsers.contains(sender)) { // Se ignora el mensaje si el remitente está bloqueado
	                	System.out.println(msg.getMessage());  
	                }
	            }
	        } 
			catch (IOException e) {
	            if (carryOn) {
	                System.out.println("[ERROR - Client] Fallo al recibir mensajes ->" + e.getMessage());
	            }
	        } 
	        catch (ClassNotFoundException e) {
	            System.out.println("[ERROR - Client] Fallo al recibir mensajes ->" + e.getMessage());
	        }
		}
		
	}

}

