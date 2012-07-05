<<<<<<< HEAD
package socket.akka 
=======
package socket.akka
>>>>>>> 40847b96c397c59d34b0b097a5efcbfee7417d67

import java.io.File
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import akka.actor.IO.ReadHandle
import akka.actor.IO.SocketHandle
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.IO
import akka.actor.IOManager
import akka.actor.Props
import akka.event.Logging
import akka.routing.RoundRobinRouter
import akka.util.ByteString
<<<<<<< HEAD
=======
import socket.akka.SocketEvents
>>>>>>> 40847b96c397c59d34b0b097a5efcbfee7417d67
import net.jxta.endpoint.EndpointAddress
import net.jxta.endpoint.Message
import net.jxta.id.IDFactory
import net.jxta.impl.endpoint.EndpointServiceImpl
import net.jxta.peergroup.PeerGroup
import net.jxta.peergroup.PeerGroupID
import net.jxta.platform.NetworkManager
import net.jxta.protocol.PipeAdvertisement
//import socket.akka.SocketWorker

sealed trait SocketMessage
case class ConnectMessage(handle: ReadHandle, bytes: ByteString, message: Message) extends SocketMessage
case class Read(handle: ReadHandle, bytes: ByteString, router: AkkaSocketServer) extends SocketMessage
case class ConnectCallback(callBack: SocketEvents, socketHandle: SocketHandle) extends SocketMessage

class AkkaSocketServer (peerGroup: PeerGroup, pipeAdv: PipeAdvertisement, callBack: SocketEvents) extends Actor {
  
	val serverName = "RdzvJxtaSocketServer"
	val rdzvPort = 9701
	val log = Logging(context.system, this)
	val serverId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, serverName.getBytes())
	val confFile = new File("." + System.getProperty("file.separator") + serverName)
	
	var netPeerGroup: PeerGroup = null
	var state:Integer = 0
	var srcAddress: EndpointAddress = null
	var dstAddress: EndpointAddress = null
	var connection: SocketHandle = null
	
	val worker = context.actorOf(Props[SocketWorker].withRouter(RoundRobinRouter(10)), name = "SocketWorker")

	override def preStart {
		log info "### AkkaSocketServer Start"
		val inetSocket = new InetSocketAddress(rdzvPort)
		IOManager(context.system) listen inetSocket
		log info "### Listening " + inetSocket.getHostName() + ":" + inetSocket.getPort()
		
		if(peerGroup == null || pipeAdv == null || callBack == null){
			NetworkManager.RecursiveDelete(confFile)
			val netManager = new NetworkManager(NetworkManager.ConfigMode.RENDEZVOUS, serverName, confFile.toURI())
			val netConfigurator = netManager.getConfigurator()
	        
	        netConfigurator.setTcpPort(rdzvPort)
	        netConfigurator.setTcpEnabled(true)
	        netConfigurator.setTcpIncoming(true)
	        netConfigurator.setTcpOutgoing(true)
	        netConfigurator.setPeerID(serverId)
	        netConfigurator.save()
					
			netPeerGroup = netManager.startNetwork()
		}else{
		    netPeerGroup = peerGroup
		}
	}

	def receive = {
		case IO.NewClient(server) =>		  	
			log info state + " ### Accept: " + server
		    server.accept()			
			state = state.+(1)
			
		case IO.Read(readHandle, bytes) =>
			log info state + " ### Read"
			worker ! Read(readHandle, bytes,this)			
			
		case IO.Listening(server, address) =>		  	
			log info "### Listening " + address			
		
		case IO.Connected(socket, address) =>			
			log info "### Connected " + address
			if(callBack != null)
				worker ! ConnectCallback(callBack, socket.asSocket)

		case IO.Closed(socket: IO.SocketHandle, cause) =>
			log info "### Socket Closed - cause: " + cause
			socket close
		  
		case IO.Closed(server: IO.ServerHandle, cause) =>
			log info "### Server Socket Closed! - cause: " + cause
			server close
			
		case ConnectMessage(readHandle, bytes, message) =>
			log info state + " ### ConnectMessage"
			state = state.+(1)
		  				
		  	val srcAddressElement = message.getMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS,EndpointServiceImpl.MESSAGE_SOURCE_NAME)
		  	srcAddress = new EndpointAddress(srcAddressElement.toString())
		  				
		  	val dstAddressElement = message.getMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS,EndpointServiceImpl.MESSAGE_DESTINATION_NAME)		  				
		  	dstAddress = new EndpointAddress(dstAddressElement.toString())
		  				
		  	// connect to the sender, receive and reply welcome messages
		  	val remoteHost = dstAddress.getProtocolAddress().split(":")		  				
		  	connection = IOManager(context.system).connect(remoteHost.apply(0), Integer.valueOf(remoteHost.apply(1)))
		  
		case _ =>
			log info "received unknown message"

	}
	
	def write(socketHandle: SocketHandle, bytes: ByteBuffer){
		socketHandle write ByteString.apply(bytes)
	}
	
}

object AkkaSocketServer extends App{
	ActorSystem().actorOf(Props(new AkkaSocketServer(null,null,null)))
}