import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

import scala.actors.Actor.actor
import scala.actors.Actor

case class Echo1(socket: Socket)

object Service1 extends Actor {

	implicit def inputStreamWrapper(in: InputStream) =	new BufferedReader(new InputStreamReader(in))

	implicit def outputStreamWrapper(out: OutputStream) = new PrintWriter(new OutputStreamWriter(out))

	def echo(in: BufferedReader, out: PrintWriter) {		
		val line = in.readLine()
		out.println(line)
		out.flush()
		println("### value: " + line )
	}

	def act() {
		loop {
			receive {
			case Echo1(socket) =>
				actor {
					echo(socket.getInputStream(), socket.getOutputStream())
					socket.close
				}
			}
		}
	}
}

object EchoServer1 extends App{
	Service1.start
	println("1");
	val serverSocket = new ServerSocket(7400)
	println("2");
	
	def start() {
	  println("3");
		while(true) {
			println("about to block")
			val clientSocket = serverSocket.accept()
			Service1 ! Echo1(clientSocket)
			println("back from actor")
		}
	}
	
	EchoServer1.start
}