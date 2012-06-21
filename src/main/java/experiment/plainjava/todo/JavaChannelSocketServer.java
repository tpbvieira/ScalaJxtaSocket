package experiment.plainjava.todo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaChannelSocketServer {

	private static int i = 1;
	public static final int serverPort = 7400;
	AtomicBoolean isRunning = new AtomicBoolean(true);
	private ServerSocketChannel ssc ;

	public JavaChannelSocketServer(){
	}

	public void setup(int port) throws IOException {
		System.out.println("### Starting JavaChannelSocketServer");
		ssc = ServerSocketChannel.open();
		InetSocketAddress address = new InetSocketAddress("localhost", port);
		ssc.socket().bind(address);
		while(true){
			this.serve();	
		}		
	}

	private void serve() throws IOException {
		System.out.println("\n### Receiving #" + i++);
		long start = System.currentTimeMillis();
		
		SocketChannel channel = ssc.accept();
		ByteBuffer buffer = ByteBuffer.allocate(12);		
		channel.read(buffer);		
		buffer.clear();		
		
		int iterations = buffer.getInt();
		int dataSize = buffer.getInt();
		int totalSize = iterations * dataSize;

		System.out.println("### Iterations: " + iterations);
		System.out.println("### Size: " + dataSize);
		
		buffer = ByteBuffer.allocate(dataSize);
		buffer.clear();		

		System.out.println(MessageFormat.format("### {0}# Sending/Receiving {1} bytes in {2} times.", 
				Thread.currentThread().getId(), totalSize, iterations));

		int read, wrote;
		for (int i = 0; i < iterations; i++) {
			read = channel.read(buffer);
			wrote = channel.write(buffer);
			System.out.println("### " + i + "# r:" + read + " w:" + wrote);
		}

		channel.close();		
		long finish = System.currentTimeMillis();
		long elapsed = finish - start;

		System.out.println("### " + Thread.currentThread().getId() + "# Socket closed");
		System.out.println(MessageFormat.format("### {3}# Received {0} bytes in {1} ms. Throughput = {2} KB/sec.", 
				totalSize, elapsed,	(totalSize / elapsed) * 1000 / 1024, Thread.currentThread().getId()));
	}

	public static void main(String args[]) {
		JavaChannelSocketServer blockingServerSocket = new JavaChannelSocketServer();
		try {
			blockingServerSocket.setup(serverPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}