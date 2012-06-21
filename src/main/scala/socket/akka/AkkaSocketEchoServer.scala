package socket.akka; 

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.IO
import akka.actor.IOManager
import akka.actor.Props
import akka.event.Logging

class AkkaSocketEchoServer(port: Int) extends Actor {
  
	val log = Logging(context.system, this)

	override def preStart {
		IOManager(context.system) listen new InetSocketAddress(port)
		println("Started!")
	}

	def receive = {
		case IO.NewClient(server) =>
			server.accept()
			println("### Accpept" + server)
			
		case IO.Read(readHandle, bytes) =>
			println("### Read")
		  	readHandle.asSocket write bytes.compact
			println("### Data sent back")

		case IO.Listening(server, address) =>
		  println("### Listening" + address)
		
		case IO.Connected(socket, address) =>
		  println("### Connected" + address) 

		case IO.Closed(socket: IO.SocketHandle, cause) =>
		  println("### Socket Closed!" + cause)
		  
		case IO.Closed(server: IO.ServerHandle, cause) =>
		  println("### Server Socket Closed!" + cause)
		  
		case _ =>
		  log info "received unknown message"

	}	
}

object AkkaSocketEchoServer extends App{
	ActorSystem().actorOf(Props(new AkkaSocketEchoServer(9701)))
}