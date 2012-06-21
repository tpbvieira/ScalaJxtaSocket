import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ReadOnlyBufferException
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.IO
import akka.actor.IOManager
import akka.actor.Props
import akka.event.Logging
import akka.util.ByteString
import net.jxta.document.AdvertisementFactory
import net.jxta.document.StructuredDocumentFactory
import net.jxta.document.XMLDocument
import net.jxta.document.XMLElement
import net.jxta.endpoint.MessageElement
import net.jxta.id.IDFactory
import net.jxta.impl.endpoint.msgframing.WelcomeMessage
import net.jxta.parser.exceptions.JxtaBodyParserException
import net.jxta.parser.exceptions.JxtaHeaderParserException
import net.jxta.parser.exceptions.JxtaWelcomeParserException
import net.jxta.parser.JxtaParser
import net.jxta.peergroup.PeerGroupID
import net.jxta.id.ID
import scala.collection.immutable.HashMap
import net.jxta.document.StructuredDocument
import net.jxta.endpoint.EndpointAddress
import net.jxta.impl.endpoint.EndpointServiceImpl
import scala.collection.immutable.StringOps
import net.jxta.endpoint.Message
import net.jxta.endpoint.StringMessageElement
import net.jxta.endpoint.TextDocumentMessageElement
import java.io.File
import net.jxta.platform.NetworkConfigurator
import net.jxta.platform.NetworkManager
import net.jxta.peergroup.PeerGroup
import net.jxta.document.MimeMediaType

class AkkaSocketServer(port: Int) extends Actor {
  
	val serverName = "RdzvJxtaSocketServer"
	val rdzvPort = 9701
	val log = Logging(context.system, this)
	val serverId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, serverName.getBytes())		
	var netPeerGroup: PeerGroup = null
	val confFile = new File("." + System.getProperty("file.separator") + serverName)

	override def preStart {
		IOManager(context.system) listen new InetSocketAddress(port)
		println("Started!")
		
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
	}

	def receive = {
		case IO.NewClient(server) =>
			server.accept()
			println("### Accpept server: " + server)
			
		case IO.Read(readHandle, bytes) =>
			println("### Read!")
		  	try{		  	  
		  	  val welcome = JxtaParser.welcomeParser(bytes.compact.asByteBuffer)		  	  
		  	  println("### welcome: " + new String(welcome.getByteBuffer().array()))
		  	  
		  	  val pubAdd = welcome.getPublicAddress
		  	  val dstAdd = welcome.getDestinationAddress()
		  	  val peerId = welcome.getPeerID()		  	  
		  	  val replyMessage = new WelcomeMessage(pubAdd, dstAdd, serverId, false)
		  	  
		  	  readHandle.asSocket write ByteString.apply(replyMessage.getByteBuffer())
		  	  print("### reply: " + new String(replyMessage.getByteBuffer().array()))		  	  
		  	}catch {
		  	  
		  		case e: IOException => 
		  		  	e.printStackTrace()
		  		
		  		case e: JxtaWelcomeParserException =>
		  			val message = JxtaParser.messageParser(bytes.compact.asByteBuffer)
		  			println("### Message: " + new String(bytes.toByteBuffer.array()))
		  			
		  			if (message.getMessageElement("jxta", "Connect") != null){
		  				println("### ConnectMessage!")	
		  				val dstAddressElement = message.getMessageElement(	EndpointServiceImpl.MESSAGE_DESTINATION_NS,
		  																	EndpointServiceImpl.MESSAGE_DESTINATION_NAME)
		  				val dstAddress = new EndpointAddress(dstAddressElement.toString())
		  				val remoteHost = dstAddress.getProtocolAddress().split(":")		  				
		  				val socket = IOManager(context.system).connect(remoteHost.apply(0), Integer.valueOf(remoteHost.apply(1)))
		  				
		  				val msg = new Message()		  				
		  				msg.addMessageElement("jxta", new TextDocumentMessageElement("RdvAdvReply", netPeerGroup.getPeerAdvertisement().getDocument(MimeMediaType.XMLUTF8), null))
		  				msg.addMessageElement("jxta", new StringMessageElement("ConnectedPeer", netPeerGroup.getPeerID().toString(), null))
		  				msg.addMessageElement("jxta", new StringMessageElement("ConnectedLease", "1200000", null))
		  			}
		  	  	
		  		case e: JxtaHeaderParserException => 
		  		  	e.printStackTrace()
		  		
		  	  	case e: JxtaBodyParserException => 
		  	  	  	e.printStackTrace()
		  	  	
		  	  	case e: ReadOnlyBufferException => 
		  	  	  	e.printStackTrace()
		  		
		  	  	case e: Exception => 
		  	  	  	e.printStackTrace()
			}

		case IO.Listening(server, address) =>
		  println("### Listening " + address)
		
		case IO.Connected(socket, address) =>
		  println("### Connected " + address) 

		case IO.Closed(socket: IO.SocketHandle, cause) =>
		  println("### Socket Closed! - cause: " + cause)
		  
		case IO.Closed(server: IO.ServerHandle, cause) =>
		  println("### Server Socket Closed! - cause: " + cause)
		  
		case _ =>
		  log info "received unknown message"

	}	
}

object AkkaSocketServer extends App{
	ActorSystem().actorOf(Props(new AkkaSocketServer(9701)))
}