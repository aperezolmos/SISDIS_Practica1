package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interfaz que define la signatura de los métodos de envío de mensaje, desconexión y arranque.
 * 
 * @author Amanda Pérez Olmos
 */
public interface ChatClient {

	/**
     * Inicia la conexión con el servidor y comienza a escuchar mensajes.
     * 
     * @return <b>true</b> si la conexión es exitosa, <b>false</b> en caso de error
     */
	public boolean start();
	
	/**
     * Envía un mensaje al servidor a través del flujo de salida.
     * 
     * @param msg Mensaje a enviar
     */
	public void sendMessage(ChatMessage msg);
	
	/**
	 * Desconecta al cliente del servidor y libera los recursos.
	 */
	public void disconnect();
}
