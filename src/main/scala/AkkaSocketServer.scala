import java.net.InetSocketAddress

import akka.actor._

class AkkaSocketServer(port: Int) extends Actor {

	override def preStart {
		IOManager(context.system) listen new InetSocketAddress(port)
		println("preStart!")
	}

	def receive = {
		case IO.NewClient(server) =>
			server.accept()
			println("### Accpept!" + server)
			
		case IO.Read(readHandle, bytes) =>
			println("### Read!!")
		  	readHandle.asSocket write bytes.compact
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

object AkkaSocketServer extends App{
	println("01!")
	val port = 7400
	ActorSystem().actorOf(Props(new AkkaSocketServer(port)))
	println("03!")
}