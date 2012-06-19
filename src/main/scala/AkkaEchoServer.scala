import java.net.InetSocketAddress

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.IO
import akka.actor.IOManager
import akka.actor.Props

class AkkaEchoServer(port: Int) extends Actor {

  val state = IO.IterateeRef.Map.async[IO.Handle]()(context.dispatcher)

  override def preStart {
    IOManager(context.system) listen new InetSocketAddress(port)
  }

  def receive = {
    
  	case IO.NewClient(server) =>
  	  println("### NewClient")
      val socket = server.accept()      
      state(socket) flatMap (_ => AkkaEchoServer.processRequest(socket))
      
    case IO.Read(socket, bytes) =>
      println("### Read")
      state(socket)(IO.Chunk(bytes))
      
    case IO.Closed(socket, cause) =>
      println("### Close")
      state(socket)(IO.EOF(None))
      state -= socket
  }
}

object AkkaEchoServer extends App{
  
  def processRequest(socket: IO.SocketHandle): IO.Iteratee[Unit] =  {
    println("### In process request")
      for {
        bs <- IO take 10
      } yield {
        println("Size: " + bs.size)
        println("I'll get here")
        socket.asSocket write bs        
        println("But not here ... as expected")        
      }    
  }

  ActorSystem().actorOf(Props(new AkkaEchoServer(7400)))
}