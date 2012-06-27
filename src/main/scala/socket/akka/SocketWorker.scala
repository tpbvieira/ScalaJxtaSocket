import java.io.IOException
import java.nio.ReadOnlyBufferException

import scala.Array.canBuildFrom

import akka.actor.Actor
import akka.actor.IOManager
import akka.event.Logging
import akka.util.ByteString
import net.jxta.document.MimeMediaType
import net.jxta.document.StructuredDocumentFactory
import net.jxta.document.TextDocument
import net.jxta.endpoint.EndpointAddress
import net.jxta.endpoint.Message
import net.jxta.endpoint.StringMessageElement
import net.jxta.endpoint.TextDocumentMessageElement
import net.jxta.endpoint.WireFormatMessageFactory
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader
import net.jxta.impl.endpoint.msgframing.WelcomeMessage
import net.jxta.impl.endpoint.EndpointServiceImpl
import net.jxta.parser.exceptions.JxtaBodyParserException
import net.jxta.parser.exceptions.JxtaHeaderParserException
import net.jxta.parser.exceptions.JxtaWelcomeParserException
import net.jxta.parser.JxtaParser
import util.AkkaUtil

class Worker extends Actor {
  
	val log = Logging(context.system, this)
  
	def receive = {
	  
	  	case ConnectCallback(callBack, socketHandle) => 
	  		callBack.onConnect(socketHandle)
			
		case Read(readHandle, bytes, router) =>
		  	try{		  	  
		  	  val welcome = JxtaParser.welcomeParser(bytes.compact.asByteBuffer)		  	  
		  	  log info router.state + " ### welcome: " + new String(welcome.getByteBuffer().array())
		  	  router.state = router.state.+(1)
		  	  
		  	  val pubAdd = welcome.getPublicAddress
		  	  val dstAdd = welcome.getDestinationAddress()
		  	  val replyMessage = new WelcomeMessage(pubAdd, dstAdd, router.serverId, false)
		  	  
		  	  readHandle.asSocket write ByteString.apply(replyMessage.getByteBuffer())
		  	  log info router.state + " ### reply: " + new String(replyMessage.getByteBuffer().array())
		  	  router.state = router.state.+(1)

		  	  if(router.state == 6){
		  		  log info router.state + " ### ConnectedPeer"		  		  
		  		  //RdvAdvReply, ConnectedPeer and ConnectedLease messages
		  		  val msg = new Message()		  				
		  		  val rdvAdvReplyDoc = router.netPeerGroup.getPeerAdvertisement().getDocument(MimeMediaType.XMLUTF8).asInstanceOf[TextDocument]
  				  msg.addMessageElement("jxta", new TextDocumentMessageElement("RdvAdvReply", rdvAdvReplyDoc, null))		  				
				  msg.addMessageElement("jxta", new StringMessageElement("ConnectedPeer", router.netPeerGroup.getPeerID().toString(), null))		  				
				  msg.addMessageElement("jxta", new StringMessageElement("ConnectedLease", "1200000", null))
				
				  // creating EndpointRouterMsg
				  var strDoc = AkkaUtil.structuredDocumentToXmldocument(StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "jxta:ERM"))
				  strDoc.addAttribute("xmlns:jxta", "http://jxta.org")
				  strDoc.addAttribute("xml:space", "preserve")		  				
				  AkkaUtil.append(strDoc, strDoc.createElement("Src", "jxta://" + router.serverId.getUniqueValue()))		  				
				  AkkaUtil.append(strDoc, strDoc.createElement("Dest", "jxta://" + router.srcAddress.getProtocolAddress() + router.dstAddress.getServiceParameter()))
				  AkkaUtil.append(strDoc, strDoc.createElement("Last", "jxta://" + router.serverId.getUniqueValue()))
				  AkkaUtil.append(strDoc, strDoc.createElement("Fwd"))
				  val rmElem = new TextDocumentMessageElement("EndpointRouterMsg", strDoc, null);
				  msg.addMessageElement("jxta", rmElem)				
				  msg.addMessageElement("jxta", new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, router.serverId.getUniqueValue().toString(), null))
				  msg.addMessageElement("jxta", new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME, dstAdd + "/EndpointService:jxta-NetGroup/EndpointRouter", null))
				  
				  val serialed = WireFormatMessageFactory.toWire(msg, WireFormatMessageFactory.DEFAULT_WIRE_MIME, null)
				  val header = new MessagePackageHeader()					
				  header.setContentTypeHeader(serialed.getMimeType())
				  header.setContentLengthHeader(serialed.getByteLength())
				  
				  val byteStr = ByteString.apply(header.getByteBuffer())				  
				  serialed.getByteBuffers() flatMap (item => byteStr.++(ByteString.apply(item)))
				  
				  router.connection.asSocket write byteStr
		  	  }
		  	  
		  	}catch {
		  	  
		  		case e: IOException => 
		  		  	e.printStackTrace()
		  		
		  		case e: JxtaWelcomeParserException =>
		  			val message = JxtaParser.messageParser(bytes.compact.asByteBuffer)
		  			if (message.getMessageElement("jxta", "Connect") != null){		  			  
		  				sender ! ConnectMessage(readHandle, bytes,message)
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

	}
}