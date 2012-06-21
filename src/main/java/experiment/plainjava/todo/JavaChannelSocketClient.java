package experiment.plainjava.todo;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.Arrays;

public class JavaChannelSocketClient {

	private final static int RUNS = 8;
	private final static int ITERATIONS = 1000;
	private final static int PAYLOADSIZE = 64 * 1024;//64kb

	public JavaChannelSocketClient() {
	}

	public void run() {
		try {
			long start = System.currentTimeMillis();
			System.out.println("### Connecting to the server");
	
			SocketChannel socketChannel = SocketChannel.open();
			socketChannel.connect(new InetSocketAddress("localhost", JavaChannelSocketServer.serverPort));
						
			int totalSize = (ITERATIONS * PAYLOADSIZE) + (Integer.SIZE/8) + (Integer.SIZE/8);
			System.out.println("### Client Sending/Receiving " + totalSize + " bytes in " + ITERATIONS + " iterations.");
			
			ByteBuffer buffer = ByteBuffer.allocate((Integer.SIZE/8) + (Integer.SIZE/8));
			buffer.clear();			
			buffer.putInt(ITERATIONS);
			buffer.putInt(PAYLOADSIZE);
			while(buffer.hasRemaining()) {
			    socketChannel.write(buffer);
			}						

			buffer = ByteBuffer.allocate(PAYLOADSIZE);
			byte[] tmpBuf = new byte[PAYLOADSIZE];
			for (int i = 0; i < ITERATIONS; i++) {
				buffer.clear();
				Arrays.fill(tmpBuf, (byte) i);
				buffer.put(tmpBuf);
				buffer.flip();
				while(buffer.hasRemaining()) {
					socketChannel.write(buffer);
				}
				
				socketChannel.read(buffer);
				
				if(!Arrays.equals(buffer.array(), tmpBuf))
					System.out.println("### Erro on Sent/Received data!");
			}

			socketChannel.close(); 
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
			Thread.currentThread().setName(JavaChannelSocketClient.class.getName() + ".main()");			
			JavaChannelSocketClient socEx = new JavaChannelSocketClient();
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