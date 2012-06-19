//package experiment.akka;
//
//import java.io.Closeable;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InterruptedIOException;
//import java.net.SocketTimeoutException;
//import java.util.LinkedList;
//import java.util.Queue;
//
//import net.jxta.endpoint.MessageElement;
//import net.jxta.endpoint.StringMessageElement;
//
//
///**
// * Provides the stream data source for JxtaSocket.
// *
// * @author Athomas Goldberg
// */
//class AkkaJxtaSocketInputStream implements Closeable {
//
//	// SKIP_BUFFER_SIZE is used to determine the size of skipBuffer
//	private static final int SKIP_BUFFER_SIZE = 2048;
//	// skipBuffer is initialized in skip(long), if needed.
//	private static byte[] skipBuffer;
//
//	/**
//	 * We push this "poison" value into the accept backlog queue in order to
//	 * signal that the queue has been closed.
//	 */
//	protected static final MessageElement QUEUE_END = new StringMessageElement("Terminal", "Terminal", null);
//
//	/**
//	 * Our read timeout.
//	 */
//	private long timeout = 60 * 1000;
//
//	/**
//	 * The associated socket.
//	 */
//	private final AkkaJxtaSocket socket;
//
//	/**
//	 * Our queue of message elements waiting to be read.
//	 */
//	protected final Queue<MessageElement> queue;
//
//	/**
//	 * The maximum number of message elements we will allow in the queue.
//	 */
//	protected final int queueSize;
//
//	/**
//	 * The current message element input stream we are processing.
//	 */
//	private InputStream currentMsgStream = null;
//
//	/**
//	 * Construct an InputStream for a specified JxtaSocket.
//	 *
//	 * @param socket  the JxtaSocket
//	 * @param queueSize the queue size
//	 */
//	AkkaJxtaSocketInputStream(AkkaJxtaSocket socket, int queueSize) {
//		this.socket = socket;
//		this.queueSize = queueSize;
//		queue = new LinkedList<MessageElement>();
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//
//	public synchronized int available() throws IOException {
//		int result;
//		InputStream in = getCurrentStream(false);
//
//		if (in != null) {
//			result = in.available();
//		} else {
//			// We chose not to block, if we have no inputstream then
//			// that means there are no bytes available.
//			result = 0;
//		}
//		return result;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//
//	public synchronized int read() throws IOException {
//		byte[] b = new byte[1];
//		int result = 0;
//
//		// The result of read() can be -1 (EOF), 0 (yes, its true) or 1.
//		while (0 == result) {
//			result = read(b, 0, 1);
//		}
//
//		if (-1 != result) {
//			result = (int) b[0];
//		}
//		return result;
//	}
//
//	public int read(byte b[]) throws IOException {
//		return read(b, 0, b.length);
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//
//	public synchronized int read(byte b[], int off, int len) throws IOException {
//		if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
//			throw new IndexOutOfBoundsException();
//		}
//
//		while (true) {
//			int result = -1;
//			InputStream in = getCurrentStream(true);
//
//			if (null == in) {
//				return -1;
//			}
//
//			result = in.read(b, off, len);
//			if (0 == result) {
//				// Some streams annoyingly return 0 result. We won't
//						// perpetuate this behaviour.
//				continue;
//			}
//
//			if (result == -1) {
//				closeCurrentStream();
//				continue;
//			}
//			return result;
//		}
//	}
//
//	public long skip(long n) throws IOException {
//
//		long remaining = n;
//		int nr;
//		if (skipBuffer == null)
//			skipBuffer = new byte[SKIP_BUFFER_SIZE];
//
//		byte[] localSkipBuffer = skipBuffer;
//
//		if (n <= 0) {
//			return 0;
//		}
//
//		while (remaining > 0) {
//			nr = read(localSkipBuffer, 0,
//					(int) Math.min(SKIP_BUFFER_SIZE, remaining));
//			if (nr < 0) {
//				break;
//			}
//			remaining -= nr;
//		}
//
//		return n - remaining;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//
//	public synchronized void close() {
//		queue.clear();
//		closeCurrentStream();
//		queue.offer(QUEUE_END);
//		notify();
//	}
//
//	/**
//	 * Rather than force the InputStream closed we add the EOF at the end of
//	 * any current data.
//	 */
//	synchronized void softClose() {
//		queue.offer(QUEUE_END);
//		notify();
//	}
//
//	/**
//	 * Get the input stream for the current segment and optionally block until
//	 * a segment is available.
//	 *
//	 * @param block If {@code true} then block until a segment is available.
//	 * @return the InputStream
//	 * @throws IOException if an io error occurs
//	 */
//	private InputStream getCurrentStream(boolean block) throws IOException {
//
//		if (currentMsgStream == null) {
//
//			if (QUEUE_END == queue.peek()) {
//				// We are at the end of the queue.
//				return null;
//			}
//
//			MessageElement me = null;
//			long pollUntil = (Long.MAX_VALUE == timeout) ? Long.MAX_VALUE : System.currentTimeMillis() + timeout;
//
//			while (pollUntil >= System.currentTimeMillis()) {
//				try {
//					me = queue.poll();
//
//					if (null == me) {
//						long sleepFor = pollUntil - System.currentTimeMillis();
//
//						if (sleepFor > 0) {
//							wait(sleepFor);
//						}
//					} else {
//						break;
//					}
//				} catch (InterruptedException woken) {
//					InterruptedIOException incomplete = new InterruptedIOException("Interrupted waiting for data.");
//
//							incomplete.initCause(woken);
//							incomplete.bytesTransferred = 0;
//							throw incomplete;
//				}
//			}
//
//			if (block && (null == me)) {
//				throw new SocketTimeoutException("Socket timeout during read.");
//			}
//
//			if (me != null) {
//				currentMsgStream = me.getStream();
//			}
//		}
//		return currentMsgStream;
//	}
//
//	private void closeCurrentStream() {
//		if (currentMsgStream != null) {
//			try {
//				currentMsgStream.close();
//			} catch (IOException ignored) {// ignored
//			}
//			currentMsgStream = null;
//		}
//	}
//
//	synchronized void enqueue(MessageElement element) {
//		if (queue.contains(QUEUE_END)) {
//			// We have already marked the end of the queue.
//			return;
//		}
//		if (queue.size() < queueSize) {
//			queue.offer(element);
//		}
//		notify();
//	}
//
//	/**
//	 * Returns the timeout value for this socket. This is the amount of time in
//	 * relative milliseconds which we will wait for read() operations to
//	 * complete.
//	 *
//	 * @return The timeout value in milliseconds or 0 (zero) for
//	 *         infinite timeout.
//	 */
//	long getTimeout() {
//		if (timeout < Long.MAX_VALUE) {
//			return timeout;
//		} else {
//			return 0;
//		}
//	}
//
//	/**
//	 * Returns the timeout value for this socket. This is the amount of time in
//	 * relative milliseconds which we will wait for read() operations to
//	 * operations to complete.
//	 *
//	 * @param timeout The timeout value in milliseconds or 0 (zero) for
//	 *                infinite timeout.
//	 */
//	void setTimeout(long timeout) {
//		if (timeout < 0) {
//			throw new IllegalArgumentException("Negative timeout not allowed.");
//		}
//
//		if (0 == timeout) {
//			timeout = Long.MAX_VALUE;
//		}
//		this.timeout = timeout;
//	}
//
//	public synchronized void mark(int readlimit) {}
//
//	public synchronized void reset() throws IOException {
//		throw new IOException("mark/reset not supported");
//	}
//
//	public boolean markSupported() {
//		return false;
//	}
//}