package es.ubu.lsi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import es.ubu.lsi.common.ChatMessage;

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
    
    // --------------------------------------------------------------------------------
	
	public ChatClientImpl(String server, String username, int port) {
		this.server = server;
		this.username = username;
		this.port = port;
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
        if (!client.start()) {
            System.out.println("ERROR - No se pudo conectar al servidor.");
        }
	}
	
	// --------------------------------------------------------------------------------
	
	@Override
	public boolean start() {
		
		try {
            socket = new Socket(server, port);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());

            output.writeObject(username); // Enviar el nombre de usuario al servidor
            System.out.println("Conectado al servidor como: " + username);

            new Thread(new ChatClientListener()).start(); // Hilo para escuchar mensajes

            Scanner scanner = new Scanner(System.in);
            
            while (carryOn) {
                String message = scanner.nextLine();
                
                if (message.equalsIgnoreCase("logout")) {
                    sendMessage(new ChatMessage(id, ChatMessage.MessageType.LOGOUT, ""));
                    carryOn = false;
                } 
                else {
                    sendMessage(new ChatMessage(id, ChatMessage.MessageType.MESSAGE, message));
                }
            }
            scanner.close();
            disconnect();
            return true;

        } 
		catch (IOException e) {
            System.out.println("ERROR al conectar con el servidor: " + e.getMessage());
            return false;
        }
	}

	@Override
	public void sendMessage(ChatMessage msg) {
		
		try {
            output.writeObject(msg);
        } 
		catch (IOException e) {
            System.out.println("ERROR enviando mensaje: " + e.getMessage());
        }
	}

	@Override
	public void disconnect() {
		
		try {
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();

            System.out.println("Desconectado del servidor.");
        } 
		catch (IOException e) {
            System.out.println("ERROR al cerrar la conexión: " + e.getMessage());
        }
	}
	
	// --------------------------------------------------------------------------------
	
	public class ChatClientListener implements Runnable {

		@Override
		public void run() {
			
			try {
	            while (carryOn) {
	                ChatMessage msg = (ChatMessage) input.readObject();
	                System.out.println(msg.getMessage());
	            }
	        } 
			catch (IOException | ClassNotFoundException e) {
	            System.out.println("Error recibiendo mensajes:" + e.getMessage());
	        }
		}
		
	}

}

