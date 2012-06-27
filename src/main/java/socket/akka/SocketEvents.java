package socket.akka;

import akka.actor.IO.SocketHandle;

public interface SocketEvents {

	public void onRead(SocketHandle socket, byte[] bytes);
	
	public void onConnect(SocketHandle socket);
	
}