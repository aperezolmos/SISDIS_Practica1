package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

public class ChatClientImpl implements ChatClient {

	public String server;
	
	public String username;
	
	public int port;
	
	public boolean carryOn = true;
	
	public int id;
	
	
	public ChatClientImpl(String server, int port, String username) {
		
	}
	
	public static void main(String[] args) {
		
	}
	
	
	@Override
	public boolean start() {
		return false;
	}

	@Override
	public void sendMessage(ChatMessage msg) {
		
	}

	@Override
	public void disconnect() {
		
	}
	
	
	public class ChatClientListener implements Runnable {

		@Override
		public void run() {
			
		}
		
	}

}

