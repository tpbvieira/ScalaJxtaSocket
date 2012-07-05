//package experiment.akka;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.logging.Level;
//
//import net.jxta.document.AdvertisementFactory;
//import net.jxta.exception.PeerGroupException;
//import net.jxta.id.IDFactory;
//import net.jxta.logging.Logging;
//import net.jxta.peer.PeerID;
//import net.jxta.peergroup.PeerGroup;
//import net.jxta.peergroup.PeerGroupID;
//import net.jxta.pipe.PipeID;
//import net.jxta.pipe.PipeService;
//import net.jxta.platform.NetworkConfigurator;
//import net.jxta.platform.NetworkManager;
//import net.jxta.protocol.PipeAdvertisement;
//import socket.akka.AkkaSocketServer;
//import socket.akka.SocketEvents;
//import akka.actor.IO.SocketHandle;
//
//public class AkkaJxtaSocketServerDriver implements SocketEvents{
//
//	private transient PeerGroup netPeerGroup = null;
//	private static final String serverName = "RdzvJxtaSocketServer";
//	public static final int rdzvPort = 9702;
//	public static final PeerID rdzvId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, serverName.getBytes());	
//	public static final File confFile = new File("." + System.getProperty("file.separator") + serverName);
//	
//	private static AkkaSocketServer serverSocket;
//	private static SocketHandle socketHandle;
//
//	public AkkaJxtaSocketServerDriver() throws IOException, PeerGroupException {
//		NetworkManager.RecursiveDelete(confFile);
//		NetworkManager netManager = new NetworkManager(NetworkManager.ConfigMode.RENDEZVOUS, serverName, confFile.toURI());
//		NetworkConfigurator netConfigurator = netManager.getConfigurator();
//
//		netConfigurator.setTcpPort(rdzvPort);
//		netConfigurator.setTcpEnabled(true);
//		netConfigurator.setTcpIncoming(true);
//		netConfigurator.setTcpOutgoing(true);
//		netConfigurator.setPeerID(rdzvId);
//		netConfigurator.save();
//
//		netPeerGroup = netManager.startNetwork();
//	}
//
//	public static PipeAdvertisement createSocketAdvertisement() {
//		PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
//		PipeID pipeId = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, serverName.getBytes());
//		advertisement.setPipeID(pipeId);
//		advertisement.setType(PipeService.UnicastType);
//		advertisement.setName("Unicast Socket");
//		return advertisement;
//	}
//
//	public void run() {
//		System.out.println("### Starting RdzvJxtaSocketServer");
//		PipeAdvertisement pipeAdv = createSocketAdvertisement();
//		serverSocket = new AkkaSocketServer(netPeerGroup, pipeAdv, this);
//	}
//
//	public static void main(String args[]) {
//		System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());
//		try {
//			Thread.currentThread().setName(AkkaJxtaSocketServerDriver.class.getName() + ".main()");
//			AkkaJxtaSocketServerDriver jxtaSocketserver = new AkkaJxtaSocketServerDriver();
//			jxtaSocketserver.run();
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void onRead(SocketHandle socket, byte[] bytes){
//		ByteBuffer bf = ByteBuffer.wrap(bytes);
//		
//		// do somothing
//		long iterations = bf.getLong();
//		int dataSize = bf.getInt();
//		System.out.println("### Iterations = " + iterations);				
//		System.out.println("### DataSize = " + dataSize);
//		
//		serverSocket.write(socketHandle,bf);
//	}
//	
//	public void onConnect(SocketHandle socket){
//		socketHandle = socket;
//	}
//	
//}