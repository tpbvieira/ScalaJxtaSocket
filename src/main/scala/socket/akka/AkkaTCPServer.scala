import java.net.InetSocketAddress

import akka.actor._

class AkkaTCPServer1(port: Int) extends Actor {

	override def preStart {
		IOManager(context.system) listen new InetSocketAddress(port)
		println("preStart!")
	}

	def receive = {
		case IO.NewClient(server) =>
			server.accept()
			println("### Accpept!")
		case IO.Read(rHandle, bytes) =>
			println("### Read!")
		  	println("### Size: " + bytes.size)
		  	println("### Iterations: " + bytes.asByteBuffer.getLong())
		  	println("### Payload: " + bytes.asByteBuffer.getInt())
			rHandle.asSocket write bytes
			println("Data sent back")
			
		case IO.Close =>
			println("### Close!")
		case IO.Closed =>
		  println("### Closed!")		  
		case IO.Connect =>
			println("### Connect!")
		case IO.Connected =>
		  println("### Connected!")		  
		case IO.Write =>
			println("### Write!")
		case IO.EOF =>
		  println("### EOF!")
	}
}

object AkkaTCPServer1 extends App{
	println("01!")
	val port = 7400
	ActorSystem().actorOf(Props(new AkkaTCPServer(port)))
	println("03!")
}