package experiment.jxta;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;
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
		System.out.println("### Starting RdzvJxtaSocketServer");
		JxtaServerSocket serverSocket = null;		
		try {
			PipeAdvertisement pipeAdv = createSocketAdvertisement();
			serverSocket = new JxtaServerSocket(netPeerGroup, pipeAdv);
			serverSocket.setSoTimeout(0);
		} catch (Exception e) {
			System.out.println("### Erro on JxtaServerSocket creation!");
			e.printStackTrace();			
			throw new RuntimeException(e);
		}

		while (true) {
			try {
				System.out.println("### Waiting for connections");
				Socket socket = serverSocket.accept();
				if (socket != null) {					
					Thread thread = new Thread(new ConnectionHandler(socket), "RdzvJxtaSocketServerConnectionHandlerThread");
					thread.start();
				}
			} catch (Exception e) {
				System.out.println("### Error accepting socket request");
				e.printStackTrace();				
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
				long totalSize = iterations * dataSize;				

				System.out.println("### Iterations: " + iterations);
				System.out.println("### Size: " + dataSize);
				
				System.out.println(MessageFormat.format("### ThreadId:{0} Sending/Receiving {1} bytes in {2} times.", 
						Thread.currentThread().getId(), totalSize, iterations));

				for (int i = 0; i < iterations; i++) {
					byte[] buf = new byte[dataSize];
					dis.readFully(buf);
					out.write(buf);
					out.flush();
				}
				
				out.close();
				in.close();
				socket.close();
				System.out.println("### ThreadId:" + Thread.currentThread().getId() + " Socket closed");
				
				long finish = System.currentTimeMillis();
				long elapsed = finish - start;

				System.out.println(MessageFormat.format("### ThreadId:{3} Received {0} bytes in {1} ms. Throughput = {2} KB/sec.", 
						totalSize, elapsed,	(totalSize / elapsed) * 1000 / 1024, Thread.currentThread().getId()));
			}  catch (IOException e) {
				e.printStackTrace();
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} 
		}

		public void run() {
			receiveData(socket);
		}
	}

	public static void main(String args[]) {
		System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());
		try {
			Thread.currentThread().setName(RdzvJxtaSocketServer.class.getName() + ".main()");
			RdzvJxtaSocketServer jxtaSocketserver = new RdzvJxtaSocketServer();
			jxtaSocketserver.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
