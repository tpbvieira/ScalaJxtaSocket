package tmp

import java.net.InetSocketAddress
import akka.actor.Actor
import akka.actor.IOManager
import akka.actor.IO

class AkkaSocketClient extends Actor {

  val address = new InetSocketAddress("remotehost", 80)
  val socket = IOManager(context.system).connect(address)
 
  def receive = {
		case IO.NewClient(server) =>
			server.accept()
			println("### Accpept!" + server)
			
		case IO.Read(rHandle, bytes) =>
			println("### Read!")
		  	println("### Size: " + bytes.size)
		  	println("### Iterations: " + bytes.asByteBuffer.getLong())
		  	println("### Payload: " + bytes.asByteBuffer.getInt())
			rHandle.asSocket.write(bytes)
			println("Data sent back")

		case IO.Listening(server, address) =>
		  println("### Listening!" + address)
		
		case IO.Connected(socket, address) =>
		  println("### Connected!" + address) 

		case IO.Closed(socket: IO.SocketHandle, cause) =>
		  println("### Socket Closed!" + cause)
		  
		case IO.Closed(server: IO.ServerHandle, cause) =>
		  println("Server Socket Closed!" + cause)

	}
  
}