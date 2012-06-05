package experiment.plainjava;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.Arrays;

public class SimpleJavaSocketClient {

	private final static long RUNS = 8;
	private final static long ITERATIONS = 1000;
	private final static int PAYLOADSIZE = 64 * 1024;//64kb

	public SimpleJavaSocketClient() {
	}

	public void run() {
		try {
			long start = System.currentTimeMillis();
			System.out.println("### Connecting to the server");
	
			Socket socket = new Socket("localhost",SimpleJavaSocketServer.serverPort);
			socket.setSoTimeout(0);
			
			// get the socket output stream
			OutputStream out = socket.getOutputStream();
			DataOutput dos = new DataOutputStream(out);

			// get the socket input stream
			InputStream in = socket.getInputStream();
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
				if(!Arrays.equals(in_buf, out_buf))
					System.out.println("### Erro on Sent/Received data!");
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
		}
	}

	public static void main(String args[]) {

		try {
			Thread.currentThread().setName(SimpleJavaSocketClient.class.getName() + ".main()");			
			SimpleJavaSocketClient socEx = new SimpleJavaSocketClient();
			System.out.println("### Connections Expected: " + RUNS);
			for (int i = 1; i <= RUNS; i++) {
				System.out.println("\n### Socket connection #" + i);
				socEx.run();
			}
			System.out.println("### Test Finished");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}