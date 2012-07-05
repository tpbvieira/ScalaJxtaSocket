package experiment.jxta;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;

import net.jxta.document.AdvertisementFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaServerSocket;

public class RdzvJxtaSocketServer {

	private transient PeerGroup netPeerGroup = null;
	private static final String serverName = "RdzvJxtaSocketServer";
	public static final int rdzvPort = 9701;
	public static final PeerID rdzvId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, serverName.getBytes());	
	public static final File confFile = new File("." + System.getProperty("file.separator") + serverName);
	
	private int connections = 0;
	
	public RdzvJxtaSocketServer() throws IOException, PeerGroupException {
		NetworkManager.RecursiveDelete(confFile);
		NetworkManager netManager = new NetworkManager(NetworkManager.ConfigMode.RENDEZVOUS, serverName, confFile.toURI());
		NetworkConfigurator netConfigurator = netManager.getConfigurator();
        
        netConfigurator.setTcpPort(rdzvPort);
        netConfigurator.setTcpEnabled(true);
        netConfigurator.setTcpIncoming(true);
        netConfigurator.setTcpOutgoing(true);
        netConfigurator.setPeerID(rdzvId);
        netConfigurator.save();
				
		netPeerGroup = netManager.startNetwork();
	}

	public static PipeAdvertisement createSocketAdvertisement() {
		PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID pipeId = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, serverName.getBytes());
		advertisement.setPipeID(pipeId);
		advertisement.setType(PipeService.UnicastType);
		advertisement.setName("Unicast Socket");
		return advertisement;
	}

	public void run() {
		System.out.println("### " + Thread.currentThread().getId() + ": Starting RdzvJxtaSocketServer");
		JxtaServerSocket serverSocket = null;		
		try {
			PipeAdvertisement pipeAdv = createSocketAdvertisement();
			serverSocket = new JxtaServerSocket(netPeerGroup, pipeAdv);
			serverSocket.setSoTimeout(0);
		} catch (Exception e) {
			System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e.getMessage());			
			throw new RuntimeException(e);
		}

		while (true) {
			try {
//				System.out.println("### " + Thread.currentThread().getId() + ": Waiting for connections");
				Socket socket = serverSocket.accept();				
				if (socket != null) {
					addConnection();
					Thread thread = new Thread(new ConnectionHandler(socket), "RdzvJxtaSocketServerConnectionHandlerThread");
					thread.start();					
				}
			} catch (Exception e) {
				System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e.getMessage());				
			}
		}
	}

	private class ConnectionHandler implements Runnable {
		Socket socket = null;

		ConnectionHandler(Socket socket) {
			this.socket = socket;
		}

		private void receiveData(Socket socket) {			
			try {
				long start = System.currentTimeMillis();

				OutputStream out = socket.getOutputStream();
				InputStream in = socket.getInputStream();
				DataInput dis = new DataInputStream(in);

				long iterations = dis.readLong();
				int dataSize = dis.readInt();
				long totalSize = iterations * dataSize * 2;				

//				System.out.println("### " + Thread.currentThread().getId() + ": Iterations: " + iterations);
//				System.out.println("### " + Thread.currentThread().getId() + ": Size: " + dataSize);				
				System.out.println("\n### " + Thread.currentThread().getId() + ": Sending/Receiving " + totalSize + " bytes in " + iterations + " times");

				for (int i = 0; i < iterations; i++) {
					byte[] buf = new byte[dataSize];
					dis.readFully(buf);
					out.write(buf);
					out.flush();
				}
				
				out.close();
				in.close();
				socket.close();
				
				long end = System.currentTimeMillis();
				long elapsed = end - start;
				System.out.println("### " + Thread.currentThread().getId() + ": Throughput = " + ((totalSize / elapsed) * 1000 / 1024) 
						+ " KB/sec  Connections = " + connections);
				subConnection();
			}  catch (IOException e) {
				System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e.getMessage());
				try {
					socket.close();
				} catch (IOException e1) {
					System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e1.getMessage());
				}
			} catch (Exception e) {
				System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e.getMessage());
				try {
					socket.close();
				} catch (IOException e1) {
					System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e1.getMessage());
				}
			} 
		}

		public void run() {
			receiveData(socket);
		}
	}
	
	private synchronized void addConnection(){
		connections++;
	}
	
	private synchronized void subConnection(){
		connections--;
	}

	public static void main(String args[]) {		
		try {
			Thread.currentThread().setName(RdzvJxtaSocketServer.class.getName() + ".main()");
			RdzvJxtaSocketServer jxtaSocketserver = new RdzvJxtaSocketServer();
			jxtaSocketserver.run();
			System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
