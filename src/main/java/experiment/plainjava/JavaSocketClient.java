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

public class JavaSocketClient {

	private static long RUNS = 10;
	private static long ITERATIONS = 50;
	private static int PAYLOADSIZE = 512 * 1024;//1kb
	private static String HOST = "localhost";
	private static boolean isAkka = true;

	public JavaSocketClient() {
	}

	public void run() {
		try {
			long start = System.currentTimeMillis();
			System.out.println("### JavaSocketClient connecting to the server");

			Socket socket = new Socket(HOST,JavaSocketServer.serverPort);
			socket.setSoTimeout(0);

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
			System.out.println("### Test Finished");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}