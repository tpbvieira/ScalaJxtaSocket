package experiment.jxta.tmp;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

import experiment.jxta.RdzvJxtaSocketServer;

import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.socket.JxtaSocket;

public class MySimpleClientThread { 

	private static int CLIENT_PORT = 8720;
	private static int ITERATIONS = 1;	
	private static int RUNS = 50;
	private static int PAYLOADSIZE = 256;

	private static final String clientName = Long.toString(System.nanoTime());
	private static final File confFile = new File("." + clientName);
	private static final PeerID peerId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, clientName.getBytes());
	private static String RDV_ADDRESS = null;

	private transient NetworkManager netManager = null;
	private transient PeerGroup netPeerGroup = null;

	public MySimpleClientThread(boolean waitForRendezvous) {
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
			System.out.println("### Connected to Rendezvouz");
		} else {
			System.out.println("### Connection connecting to Rendezvouz");
		}
	}

	public void spawn(int iterations, int size) {		
		for (int i = 0; i < iterations;) {
			Thread thread = new Thread(new SocketCLientThread(++i,size));
			thread.start();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void stop() {
		netManager.stopNetwork();
	}

	public static void main(String args[]) {
		System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());

		// Parameters
		if(args.length == 5){
			ITERATIONS = Integer.valueOf(args[0]).intValue();
			RUNS = Integer.valueOf(args[1]).intValue();
			PAYLOADSIZE = Integer.valueOf(args[2]).intValue() * 1024;
			RDV_ADDRESS = args[3];
			CLIENT_PORT = Integer.valueOf(args[4]);
		}

		Thread.currentThread().setName(MySimpleClientThread.class.getName() + ".main()");			
		boolean waitForRendezvous = true;
		final MySimpleClientThread jxtaPeer = new MySimpleClientThread(waitForRendezvous);

		for (int j = 0; j < ITERATIONS; j++) {
			jxtaPeer.spawn(RUNS,PAYLOADSIZE);
			PAYLOADSIZE = PAYLOADSIZE * 1024;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				System.out.println("### Hoocked!!");
				jxtaPeer.stop();
			}
		}
		);
	}

	private class SocketCLientThread implements Runnable{

		private int size;
		private int number;

		public SocketCLientThread(int number, int size){
			this.number = number;
			this.size = size;
		}

		public void run(){
			JxtaSocket socket = null;
			try {
				long start = System.currentTimeMillis();

				System.out.println(number + " - ### #" + Thread.currentThread().getId() + " Connecting to the server");
				socket = new JxtaSocket(netPeerGroup, RdzvJxtaSocketServer.rdzvId, RdzvJxtaSocketServer.createSocketAdvertisement(), 60000, true);
				socket.setSoTimeout(6000);
				System.out.println(number + " - ### #" + Thread.currentThread().getId() + " Connected!!");

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
				System.out.println(number + " - ### #" + Thread.currentThread().getId() + " Socket client closed");
				System.out.println(MessageFormat.format(number + " - ### #" + Thread.currentThread().getId() + " {0} bytes in {1} ms. Throughput = {2} KB/sec.", size, elapsed,(size / elapsed) * 1000 / 1024));
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
	}
}