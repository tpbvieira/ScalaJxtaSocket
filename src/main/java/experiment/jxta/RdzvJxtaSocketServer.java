package experiment.jxta;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
	public static final PeerID rdzvId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, serverName.getBytes());
	public static final int rdzvPort = 9701;
	public static final File confFile = new File("." + System.getProperty("file.separator") + serverName);
	private static int i = 1;
	
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
		System.out.println("### Starting MySimpleServer");
		JxtaServerSocket serverSocket = null;		
		try {
			PipeAdvertisement pipeAdv = createSocketAdvertisement();
			serverSocket = new JxtaServerSocket(netPeerGroup, pipeAdv, 200, 60000);
			serverSocket.setSoTimeout(60000);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("### Erro on JxtaServerSocket creation!");
			throw new RuntimeException(e);
		}

		while (true) {
			try {
				System.out.println("### Waiting for connections");
				Socket socket = serverSocket.accept();
				if (socket != null) {					
					Thread thread = new Thread(new ConnectionHandler(socket), "ServerConnectionHandlerThread");
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
			System.out.println("###" + Thread.currentThread().getId() + " New socket connection accepted: #" + i++);
			try {
				long start = System.currentTimeMillis();

				InputStream in = socket.getInputStream();
				DataInput dis = new DataInputStream(in);

				int size = dis.readInt();

				byte[] buf = new byte[size];
				dis.readFully(buf);
				in.close();

				long finish = System.currentTimeMillis();
				long elapsed = finish - start;
				if(elapsed <= 0){
					elapsed = 1;
				}
				
				socket.close();								
				System.out.println("###" + Thread.currentThread().getId() + " Connection closed");
				System.out.println(MessageFormat.format("###" + Thread.currentThread().getId() + " {0} bytes in {1} ms. Throughput = {2} KB/sec.", size, elapsed,(size / elapsed) * 1000 / 1024));
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
