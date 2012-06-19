/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
 *  
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *  
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *  
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *  
 *  ====================================================================
 *  
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */
package experiment.jxta;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaSocket;


/**
 * This tutorial illustrates the use JxtaSocket. It attempts to bind a
 * JxtaSocket to an instance of JxtaServerSocket bound socket.adv.
 * <p/>
 * Once a connection is established data is exchanged with the server.
 * The client will identify how many ITERATIONS of PAYLOADSIZE buffers will be
 * exchanged with the server and then write and read those buffers.
 */
public class SimpleSocketClient { 

	/**
	 * number of runs to make
	 */
	private static long ITERATIONS = 2;
	
	/**
	 * number of runs to make
	 */
	private static long RUNS = 100;

	/**
	 * payload size (bytes)
	 */
	private static int PAYLOADSIZE = 3 * 1024;

	private transient NetworkManager manager = null;
	private transient PeerGroup netPeerGroup = null;
	private transient PipeAdvertisement pipeAdv;    
	private transient boolean waitForRendezvous = false;

	public SimpleSocketClient(boolean waitForRendezvous) {
		try {
			manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "SocketClient", new File(new File(".cache"), "SocketClient").toURI());
			manager.startNetwork();
		} catch (Exception e) {
			e.printStackTrace();
		}

		netPeerGroup = manager.getNetPeerGroup();
		pipeAdv = JxtaSocketServer.createSocketAdvertisement();
		
		if (waitForRendezvous) {
			manager.waitForRendezvousConnection(0);
		}
	}

	/**
	 * Interact with the server.
	 */
	public void run(int size) {
		try {
			if (waitForRendezvous) {
				manager.waitForRendezvousConnection(0);
			}

			long start = System.currentTimeMillis();
			System.out.println("Connecting to the server");
			
			JxtaSocket socket = new JxtaSocket(netPeerGroup,
					// no specific peerid
					null,
					pipeAdv,
					// connection timeout: 5 seconds
					5000,
					// reliable connection
					true);

			// get the socket output stream
			socket.setSoTimeout(5 * 60 * 1000);
			OutputStream out = socket.getOutputStream();
			DataOutput dos = new DataOutputStream(out);

			dos.writeInt(size);

			byte[] out_buf = new byte[size];

			Arrays.fill(out_buf, (byte) 2);
			out.write(out_buf);
			out.flush();
			out.close();

			long finish = System.currentTimeMillis();
			long elapsed = finish - start;

			System.out.println(MessageFormat.format("EOT. Processed {0} bytes in {1} ms. Throughput = {2} KB/sec.", 
					size, elapsed,(size / elapsed) * 1000 / 1024));
			socket.close();
			System.out.println("Socket connection closed");
		} catch (Exception io) {
			io.printStackTrace();
		}
	}

	private void stop() {
		manager.stopNetwork();
	}

	/**
	 * If the java property RDVWAIT set to true then this demo
	 * will wait until a rendezvous connection is established before
	 * initiating a connection
	 *
	 * @param args none recognized.
	 */
	public static void main(String args[]) {
		System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());
		
		if(args.length == 3){
			SimpleSocketClient.ITERATIONS = Integer.valueOf(args[0]).intValue();
			SimpleSocketClient.RUNS = Integer.valueOf(args[1]).intValue();
			SimpleSocketClient.PAYLOADSIZE = Integer.valueOf(args[2]).intValue() * 1024;
		}

		try {
			Thread.currentThread().setName(SimpleSocketClient.class.getName() + ".main()");
			String value = System.getProperty("RDVWAIT", "false");
			boolean waitForRendezvous = Boolean.valueOf(value);
			SimpleSocketClient socEx = new SimpleSocketClient(waitForRendezvous);

			int i = 1;
			int k = 1;
			for (int j = 0; j < ITERATIONS; j++) {
				for (; i <= RUNS; i++) {
					System.out.println("\nSending #" + k++);
					socEx.run(PAYLOADSIZE);
				}
				PAYLOADSIZE = PAYLOADSIZE * 1024;
				i = 1;
			}			
			socEx.stop();

		} catch (Throwable e) {
			System.out.println("Failed : " + e);
			e.printStackTrace();
		}
	}
}