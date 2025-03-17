package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interfaz que define la signatura de los métodos de arranque, multidifusión, 
 * eliminación de cliente y apagado.
 * 
 * @author Amanda Pérez Olmos
 */
public interface ChatServer {

	/**
	 * Arranca el servidor y espera conexiones de clientes.
	 */
	public void startup();
	
	/**
	 * Cierra los flujos de entrada/salida y el socket correspondiente de cada cliente.
	 */
	public void shutdown();
	
	/**
	 * Retransmite un mensaje a todos los clientes conectados al servidor.
	 * 
	 * @param message Mensaje a enviar
	 */
	public void broadcast(ChatMessage message);
	
	/**
	 * Elimina un cliente de la lista.
	 * 
	 * @param id ID del cliente a eliminar
	 */
	public void remove(int id);
}
