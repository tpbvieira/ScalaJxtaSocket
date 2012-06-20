package experiment.jxta;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

public class RdzvJxtaSocketClient { 

	private static int CLIENT_PORT = 7401;
	private static long ITERATIONS = 10;	
	private static long RUNS = 10;
	private static int PAYLOADSIZE = 512 * 1024;
	private static boolean isAkka = false;

	private static final String clientName = Long.toString(System.nanoTime());
	private static final File confFile = new File("." + clientName);
	private static final PeerID peerId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, clientName.getBytes());
	private static String RDV_ADDRESS = "localhost";

	private transient NetworkManager netManager = null;
	private transient PeerGroup netPeerGroup = null;

	public RdzvJxtaSocketClient(boolean waitForRendezvous) {
		try {
			NetworkManager.RecursiveDelete(confFile);
			netManager = new NetworkManager(NetworkManager.ConfigMode.EDGE, clientName, confFile.toURI());			
			NetworkConfigurator netConfigurator = netManager.getConfigurator();

			netConfigurator.clearRendezvousSeeds();
			String address = "tcp://" + RDV_ADDRESS + ":" + RdzvJxtaSocketServer.rdzvPort;
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
			System.out.println("### Connected to Rendezvous");
		} else {
			System.out.println("### Rendezvous Connection Error!!");
		}
	}

	public void run() {
		JxtaSocket socket = null;
		try {
			long start = System.currentTimeMillis();
			System.out.println("### RdzvJxtaSocketClient connecting to the server");

			socket = new JxtaSocket(netPeerGroup, RdzvJxtaSocketServer.rdzvId, RdzvJxtaSocketServer.createSocketAdvertisement(), 60000, true);
			socket.setSoTimeout(60000);

			// get the socket output stream
			OutputStream out = socket.getOutputStream();
			DataOutput dos = new DataOutputStream(out);

			// get the socket input stream
			InputStream in = socket.getInputStream();
			DataInput dis = new DataInputStream(in);

			long totalSize = ITERATIONS * (long) PAYLOADSIZE;
			if(!isAkka){
				dos.writeLong(ITERATIONS);
				dos.writeInt(PAYLOADSIZE);
			}
			System.out.println("### Client Sending/Receiving " + totalSize + " bytes in " + ITERATIONS + " iterations.");

			for (int i = 0; i < ITERATIONS; i++) {
				byte[] out_buf = new byte[PAYLOADSIZE];
				byte[] in_buf = new byte[PAYLOADSIZE];
				Arrays.fill(out_buf, (byte) i);
				dos.write(out_buf);
				out.flush();
				dis.readFully(in_buf);
				if(!Arrays.equals(in_buf, out_buf)){
					System.out.println("??? Erro on Sent/Received data!");
				}
			}

			out.close();
			in.close();
			socket.close();
			System.out.println("### Socket connection closed");			

			long finish = System.currentTimeMillis();
			long elapsed = finish - start;
			System.out.println(MessageFormat.format("### Processed {0} bytes in {1} ms. Throughput = {2} KB/sec.", 
					totalSize, elapsed,(totalSize / elapsed) * 1000 / 1024));
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
		System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());

		// Parameters
		if(args.length >= 4){
			RDV_ADDRESS = args[0];			
			RUNS = Integer.valueOf(args[1]).intValue();
			ITERATIONS = Integer.valueOf(args[2]).intValue();			
			PAYLOADSIZE = Integer.valueOf(args[3]).intValue() * 1024;
			if(args.length > 4)
				isAkka = true;
		}

		Thread.currentThread().setName(RdzvJxtaSocketClient.class.getName() + ".main()");			
		boolean waitForRendezvous = true;
		RdzvJxtaSocketClient jxtaPeer = new RdzvJxtaSocketClient(waitForRendezvous);
		System.out.println("### Connections Expected: " + RUNS);
		for (int i = 0; i < RUNS; i++) {
			System.out.println("\n### Socket connection #" + i);
			jxtaPeer.run();
		}
		System.out.println("### Test Finished");
		jxtaPeer.stop();
	}
}
