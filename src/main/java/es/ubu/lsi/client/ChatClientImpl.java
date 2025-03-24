package es.ubu.lsi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Implementación del cliente de chat.
 * Permite a un usuario conectarse a un servidor de chat y enviar/recibir mensajes.
 * 
 * @author Amanda Pérez Olmos
 */
public class ChatClientImpl implements ChatClient {

	/** Puerto por defecto. */
	private static final int DEFAULT_PORT = 1500;
	
	/** Servidor por defecto. */
	private static final String DEFAULT_SERVER = "localhost";
	
	
	/** Dirección IP o nombre del servidor. */
	private String server;
	
	/** Nombre de usuario del cliente. */
	private String username;
	
	/** Puerto en el que se conecta al servidor. */
	private int port;
	
	/** Estado del cliente (activo o inactivo). */
	private boolean carryOn = true;
	
	/** Identificador único del cliente. */
	private int id;
	
	
	/** Socket para la conexión con el servidor. */
	private Socket socket;
	
	/** Flujo de entrada para recibir mensajes del servidor. */
	private ObjectInputStream input;
	
	/** Flujo de salida para enviar mensajes al servidor. */
    private ObjectOutputStream output;
    
    // --------------------------------------------------------------------------------
	
    /**
     * Inicializa un cliente.
     * 
     * @param server Dirección IP o nombre del servidor
     * @param port Puerto del servidor
     * @param username Nombre de usuario del cliente
     */
	public ChatClientImpl(String server, int port, String username) {
		
		this.server = server;
		this.port = port;
		this.username = username;
	}
	
	/**
	 * Método principal para ejecutar el cliente.
	 * 
	 * @param args Argumentos de línea de comandos:
	 * <ul>
	 *   <li>Dirección del servidor (opcional)</li>
	 *   <li>Nickname del usuario</li>
	 * </ul>
	 */
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

        ChatClientImpl client = new ChatClientImpl(server, port, username);
        client.start();
	}
	
	// --------------------------------------------------------------------------------
	
	@Override
	public boolean start() {
		
		try {
            socket = new Socket(server, port);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());

            // El cliente envía su nombre de usuario al servidor y el servidor le manda su ID asociado
            output.writeObject(username);
            id = ((ChatMessage) input.readObject()).getId();
            
            System.out.println("[WELCOME] Conectado al servidor como: " + username);

            new Thread(new ChatClientListener()).start(); // Hilo para escuchar mensajes

            handleInput();

            disconnect();
            return true;

        } 
		catch (IOException | ClassNotFoundException e) {
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
	
	/**
     * Maneja la entrada del usuario desde la consola y envía los mensajes al servidor.
     */
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
                sendMessage(new ChatMessage(id, MessageType.MESSAGE, "[BAN]" + username + " ha baneado a " + bannedUser));
            } 
            else if (message.startsWith("unban ")) {
            	String unbannedUser = message.substring(6).trim();
            	sendMessage(new ChatMessage(id, MessageType.MESSAGE, "[UNBAN]" + username + " ha desbaneado a " + unbannedUser));
            }
            else { // TODO: no sería del todo 'necesario' porque ahora el servidor almacena los usernames
                String signedMessage = "@" + username + ": " + message;
                sendMessage(new ChatMessage(id, MessageType.MESSAGE, signedMessage));
            }
        }
        scanner.close();
	}
	
	// --------------------------------------------------------------------------------
	
	/**
	 * Hilo de escucha para recibir mensajes del servidor.
	 */
	public class ChatClientListener implements Runnable {

		/**
		 * Escucha los mensajes del servidor (flujo de entrada) y muestra los
		 * mensajes entrantes.
		 */
		@Override
		public void run() {
			
			try {
				while (carryOn) {
					ChatMessage msg = (ChatMessage) input.readObject();
					System.out.println(msg.getMessage()); 
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

