package experiment.jxta;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.socket.JxtaSocket;

public class SimpleJxtaClient { 

	private static int CLIENT_PORT = 8720;
	private static long ITERATIONS = 1;	
	private static long RUNS = 1000;
	private static int PAYLOADSIZE = 256;

	private static final String clientName = Long.toString(System.nanoTime());
	private static final File confFile = new File("." + clientName);
	private static final PeerID peerId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, clientName.getBytes());
	private static String RDV_ADDRESS = null;
	
	private transient NetworkManager netManager = null;
	private transient PeerGroup netPeerGroup = null;

	public SimpleJxtaClient(boolean waitForRendezvous) {
		try {
			NetworkManager.RecursiveDelete(confFile);
			netManager = new NetworkManager(NetworkManager.ConfigMode.EDGE, clientName, confFile.toURI());			
			NetworkConfigurator netConfigurator = netManager.getConfigurator();

			netConfigurator.clearRendezvousSeeds();
			String address = null;
			if(RDV_ADDRESS == null)
				address = "tcp://" + "localhost" + ":" + RdzvJxtaSocketServer.rdzvPort;
			else
				address = "tcp://" + RDV_ADDRESS;
			URI rdzvUri = URI.create(address);
			netConfigurator.addSeedRendezvous(rdzvUri);

			netConfigurator.setTcpPort(CLIENT_PORT);
			netConfigurator.setTcpEnabled(true);
			netConfigurator.setTcpIncoming(true);
			netConfigurator.setTcpOutgoing(true);
			netConfigurator.setPeerID(peerId);
			netConfigurator.save();			

			netPeerGroup = netManager.startNetwork();
		} catch (IOException e) {
			e.printStackTrace();
		}catch (PeerGroupException e) {
			e.printStackTrace();
		}

		netPeerGroup.getRendezVousService().setAutoStart(false);
		if (netManager.waitForRendezvousConnection(120000)) {
			System.out.println("### Connected");
		} else {
			System.out.println("### Connection Error!!");
		}
	}

	public void run(int size) {
		JxtaSocket socket = null;
		try {
			long start = System.currentTimeMillis();
			System.out.println("### Connecting to the server");

			socket = new JxtaSocket(netPeerGroup, RdzvJxtaSocketServer.rdzvId, RdzvJxtaSocketServer.createSocketAdvertisement(), 60000, true);
			socket.setSoTimeout(6000);
			
			OutputStream out = socket.getOutputStream();
			DataOutput dos = new DataOutputStream(out);
			dos.writeInt(size);

			byte[] buffer = new byte[size];

			Arrays.fill(buffer, (byte) 2);
			out.write(buffer);
			out.flush();
			out.close();

			long finish = System.currentTimeMillis();
			long elapsed = finish - start;

			socket.close();
			System.out.println("### Socket client closed");
			System.out.println(MessageFormat.format("### {0} bytes in {1} ms. Throughput = {2} KB/sec.", size, elapsed,(size / elapsed) * 1000 / 1024));
		} catch (Exception io) {
			io.printStackTrace();
			try{
				if(socket != null)
					socket.close();
			} catch (Exception e) {
				io.printStackTrace();
			}
		}
	}

	private void stop() {
		netManager.stopNetwork();
	}

	public static void main(String args[]) {
		int k = 1;
		System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());

		// Parameters
		if(args.length == 5){
			ITERATIONS = Integer.valueOf(args[0]).intValue();
			RUNS = Integer.valueOf(args[1]).intValue();
			PAYLOADSIZE = Integer.valueOf(args[2]).intValue() * 1024;
			RDV_ADDRESS = args[3];
			CLIENT_PORT = Integer.valueOf(args[4]);
		}

		Thread.currentThread().setName(SimpleJxtaClient.class.getName() + ".main()");			
		boolean waitForRendezvous = true;
		SimpleJxtaClient jxtaPeer = new SimpleJxtaClient(waitForRendezvous);

		for (int j = 0; j < ITERATIONS; j++) {
			for (int i = 0; i < RUNS; i++) {
				System.out.println("### Sending #" + k++);
				jxtaPeer.run(PAYLOADSIZE);
			}
			PAYLOADSIZE = PAYLOADSIZE * 1024;
		}			
		jxtaPeer.stop();

	}
}
