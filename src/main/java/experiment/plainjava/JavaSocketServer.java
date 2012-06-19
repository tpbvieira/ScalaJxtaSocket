package experiment.plainjava;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;

public class JavaSocketServer {

	public static final int serverPort = 7400;

	public JavaSocketServer(){
		
	}

	public void run() {
		System.out.println("### Starting JavaSocketServer");
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(serverPort, 10);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		int i = 1;
		while (true) {
			try {
				System.out.println("### Waiting for connections");
				serverSocket.setSoTimeout(0);
				Socket socket = serverSocket.accept();
				if (socket != null) {
					System.out.println("\n### Receiving #" + i++);
					Thread thread = new Thread(new ConnectionHandler(socket), "ConnectionHandlerThread");
					thread.start();
				}else{
					System.out.println("### Socket null!! \nStill waiting...");
				}
			} catch (IOException e) {
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
			try {
				long start = System.currentTimeMillis();

				OutputStream out = socket.getOutputStream();
				InputStream in = socket.getInputStream();
				DataInput dis = new DataInputStream(in);

				long iterations = dis.readLong();
				int dataSize = dis.readInt();
				long totalSize = iterations * dataSize;				

				System.out.println("### Iterations: " + iterations);
				System.out.println("### Size: " + dataSize);
				
				System.out.println(MessageFormat.format("### {0}# Sending/Receiving {1} bytes in {2} times.", 
						Thread.currentThread().getId(), totalSize, iterations));

				for (int i = 0; i < iterations; i++) {
					byte[] buf = new byte[dataSize];
					dis.readFully(buf);
					out.write(buf);
					out.flush();
				}
				
				out.close();
				in.close();
				socket.close();
				System.out.println("### " + Thread.currentThread().getId() + "# Socket closed");
				
				long finish = System.currentTimeMillis();
				long elapsed = finish - start;

				System.out.println(MessageFormat.format("### {3}# Received {0} bytes in {1} ms. Throughput = {2} KB/sec.", 
						totalSize, elapsed,	(totalSize / elapsed) * 1000 / 1024, Thread.currentThread().getId()));
			} catch (Exception ie) {
				ie.printStackTrace();
			} 
		}

		public void run() {
			receiveData(socket);
		}
	}

	public static void main(String args[]) {		
		try {
			Thread.currentThread().setName(JavaSocketServer.class.getName() + ".main()");
			JavaSocketServer socEx = new JavaSocketServer();
			socEx.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}