package experiment.plainjava;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class JavaSocketClient {

	private static long ITERATIONS = 1;
	private static long RUNS = 10;
	private static int PAYLOADSIZE = 1024 * 1024;
	private static boolean isAkka = false;

	private static String HOST = "localhost";

	public void run() {
		try {
			Socket socket = new Socket(HOST,JavaSocketServer.javaSocketServerPort);
			socket.setSoTimeout(0);
			System.out.println("### JavaSocketClient connected to server");

			// get the socket output stream
			OutputStream out = socket.getOutputStream();
			DataOutput dos = new DataOutputStream(out);

			// get the socket input stream
			InputStream in = socket.getInputStream();
			DataInput dis = new DataInputStream(in);

			long start = System.currentTimeMillis();
			long totalSize = ITERATIONS * (long) PAYLOADSIZE;
			if(!isAkka){
				dos.writeLong(ITERATIONS);
				dos.writeInt(PAYLOADSIZE);
			}
			System.out.println("### Sending/Receiving " + totalSize + " bytes in " + ITERATIONS + " iterations.");

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
			
			long end = System.currentTimeMillis();
			long elapsed = end - start;
			System.out.println("### Throughput = " + ((totalSize / elapsed) * 1000 / 1024) + " KB/sec");
		} catch (Exception io) {
			io.printStackTrace();
		}
	}

	public static void main(String args[]) {

		if(args.length >= 4){
			HOST = args[0];
			RUNS = Integer.valueOf(args[1]).intValue();
			ITERATIONS = Integer.valueOf(args[2]).intValue();			
			PAYLOADSIZE = Integer.valueOf(args[3]).intValue() * 1024;
			if(args.length > 4)
				isAkka = true;
		}

		try {
			Thread.currentThread().setName(JavaSocketClient.class.getName() + ".main()");			
			JavaSocketClient socEx = new JavaSocketClient();
			System.out.println("### Connections Expected: " + RUNS);
			for (int i = 1; i <= RUNS; i++) {
				System.out.println("\n### Socket connection #" + i);
				socEx.run();
			}
			System.out.println("\n\n### Test Finished");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}