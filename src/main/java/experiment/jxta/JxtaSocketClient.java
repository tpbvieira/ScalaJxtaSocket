package experiment.jxta;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

import net.jxta.exception.PeerGroupException;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaSocket;

public class JxtaSocketClient {

	private final static long RUNS = 1;
	private final static long ITERATIONS = 100;
	private final static int PAYLOADSIZE = 1024;//64kb
	private transient NetworkManager manager = null;
	private transient PeerGroup netPeerGroup = null;
	private transient PipeAdvertisement pipeAdv;
	private String clientName = "JxtaSocketClient";

	public JxtaSocketClient() {
		try {
			manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, clientName, new File(new File(".cache"), clientName).toURI());
			manager.startNetwork();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PeerGroupException e) {
			e.printStackTrace();
		}		
		netPeerGroup = manager.getNetPeerGroup();
		pipeAdv = JxtaSocketServer.createSocketAdvertisement();
	}

	public void run() {
		try {
			long start = System.currentTimeMillis();
			System.out.println("### Connecting to the server");
	
			JxtaSocket clientSocket = new JxtaSocket(netPeerGroup,					
					null,// no specific peer id
					pipeAdv,// ...pipe advertisement					
					5000,// connection timeout: 5 seconds					
					true);// reliable connection		
//			SocketAddress socketAddr = new InetSocketAddress("localhost", 4700);
//			JxtaSocket socket = new JxtaSocket(socketAddr);					
			clientSocket.setSoTimeout(0);			
			System.out.println("### Client: " + clientSocket.getLocalSocketAddress());
			System.out.println("### InetAddress: " + clientSocket.getInetAddress());
			
			// get the socket output stream
			OutputStream out = clientSocket.getOutputStream();
			DataOutput dos = new DataOutputStream(out);

			// get the socket input stream
			InputStream in = clientSocket.getInputStream();
			DataInput dis = new DataInputStream(in);			
			
			long totalSize = ITERATIONS * (long) PAYLOADSIZE;
			dos.writeLong(ITERATIONS);
			dos.writeInt(PAYLOADSIZE);
			System.out.println("### Client Sending/Receiving " + totalSize + " bytes in " + ITERATIONS + " iterations.");

			for (int i = 0; i < ITERATIONS; i++) {
				byte[] out_buf = new byte[PAYLOADSIZE];
				byte[] in_buf = new byte[PAYLOADSIZE];

				Arrays.fill(out_buf, (byte) i);
				out.write(out_buf);
				out.flush();
				dis.readFully(in_buf);
				assert Arrays.equals(in_buf, out_buf);
			}

			out.close();
			in.close();
			clientSocket.close();
			System.out.println("### Socket closed");			

			long finish = System.currentTimeMillis();
			long elapsed = finish - start;
			System.out.println(MessageFormat.format("### Processed {0} bytes in {1} ms. Throughput = {2} KB/sec.", 
					totalSize, elapsed,(totalSize / elapsed) * 1000 / 1024));
		} catch (Exception io) {
			io.printStackTrace();
		}
	}

	private void stop() {
		manager.stopNetwork();
	}

	public static void main(String args[]) {
		System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());
		try {
			Thread.currentThread().setName(JxtaSocketClient.class.getName() + ".main()");			
			JxtaSocketClient socEx = new JxtaSocketClient();
			System.out.println("### Connections Expected: " + RUNS);
			for (int i = 1; i <= RUNS; i++) {
				System.out.println("\n### Socket connection #" + i);
				socEx.run();
			}
			socEx.stop();
			System.out.println("### Test Finished");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}