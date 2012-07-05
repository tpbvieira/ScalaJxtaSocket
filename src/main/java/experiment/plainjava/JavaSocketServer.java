package experiment.plainjava;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class JavaSocketServer {

	public static final int javaSocketServerPort = 9701;
	private int connections = 0;

	public void run() {
		System.out.println("### " + Thread.currentThread().getId() + ": Starting JavaSocketServer");
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(javaSocketServerPort, 10);
		} catch (IOException e) {
			System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e.getMessage());
		}

		while (true) {
			try {
//				System.out.println("### " + Thread.currentThread().getId() + ": Waiting for connections");
				serverSocket.setSoTimeout(0);
				Socket socket = serverSocket.accept();				
				if (socket != null) {
					addConnection();
					Thread thread = new Thread(new ConnectionHandler(socket), "JavaSocketServerConnectionHandlerThread");
					thread.start();					
				}else{
					System.out.println("### Error accepting new connection!! \nStill waiting...");
				}
			} catch (IOException e) {
				System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e.getMessage());
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
				long totalSize = iterations * dataSize * 2;				

//				System.out.println("### " + Thread.currentThread().getId() + ": Iterations: " + iterations);
//				System.out.println("### " + Thread.currentThread().getId() + ": Size: " + dataSize);				
				System.out.println("\n### " + Thread.currentThread().getId() + ": Sending/Receiving " + totalSize + " bytes in " + iterations + " times");

				for (int i = 0; i < iterations; i++) {
					byte[] buf = new byte[dataSize];
					dis.readFully(buf);
					out.write(buf);
					out.flush();
				}
				
				out.close();
				in.close();
				socket.close();
				
				long end = System.currentTimeMillis();
				long elapsed = end - start;
				System.out.println("### " + Thread.currentThread().getId() + ": Throughput = " + ((totalSize / elapsed) * 1000 / 1024) 
						+ " KB/sec  Connections = " + connections);
				subConnection();
			} catch (Exception e) {
				System.out.println("### " + Thread.currentThread().getId() + ": Error = " + e.getMessage());
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
	
	private synchronized void addConnection(){
		connections++;
	}
	
	private synchronized void subConnection(){
		connections--;
	}
}