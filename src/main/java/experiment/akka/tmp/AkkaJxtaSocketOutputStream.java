package experiment.akka.tmp;
//package experiment.akka;
//
//import java.io.Closeable;
//import java.io.Flushable;
//import java.io.IOException;
//import java.net.SocketException;
//
//
///**
// * This class implements a buffered output stream. By setting up such an output
// * stream, an application can write bytes to the underlying output stream
// * without necessarily causing a call to the underlying system for each byte
// * written. Data buffer is flushed to the underlying stream, when it is full,
// * or an explicit call to flush is made.
// */
//class AkkaJxtaSocketOutputStream implements Closeable, Flushable{
//
//    /**
//     * If {@code true} then this socket is closed.
//     */
//    protected boolean closed = false;
//
//    /**
//     * Data buffer
//     */
//    protected byte buf[];
//
//    /**
//     * byte count in buffer
//     */
//    protected int count;
//
//    /**
//     * JxtaSocket associated with this stream
//     */
//    protected AkkaJxtaSocket socket;
//
//    /**
//     * Constructor for the JxtaSocketOutputStream object
//     *
//     * @param socket JxtaSocket associated with this stream
//     * @param size   buffer size in bytes
//     */
//    public AkkaJxtaSocketOutputStream(AkkaJxtaSocket socket, int size) {
//        if (size <= 0) {
//            throw new IllegalArgumentException("Buffer size <= 0");
//        }
//        buf = new byte[size];
//        this.socket = socket;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    
//    public synchronized void close() throws IOException {
//        flushBuffer();
//        closed = true;
//    }
//
//    /**
//     * Similar to close except that any buffered data is discarded.
//     */
//    synchronized void hardClose() {
//        count = 0;
//        closed = true;
//    }
//
//    /**
//     * Flush the internal buffer
//     *
//     * @throws IOException if an i/o error occurs
//     */
//    private void flushBuffer() throws IOException {
//        if (count > 0) {
//            // send the message
//            socket.write(buf, 0, count);
//            count = 0;
//        }
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    
//    public synchronized void write(int b) throws IOException {
//
//        if (closed) {
//            throw new SocketException("Socket Closed.");
//        }
//
//        if (count >= buf.length) {
//            flushBuffer();
//        }
//        buf[count++] = (byte) b;
//    }
//
//    public void write(byte b[]) throws IOException {
//    	write(b, 0, b.length);
//        }
//    
//    /**
//     * {@inheritDoc}
//     */
//    
//    public synchronized void write(byte b[], int off, int len) throws IOException {
//        int left = buf.length - count;
//
//        if (closed) {
//            throw new SocketException("Socket Closed.");
//        }
//
//        if (len > left) {
//            System.arraycopy(b, off, buf, count, left);
//            len -= left;
//            off += left;
//            count += left;
//            flushBuffer();
//        }
//
//        // chunk data if larger than buf.length
//        while (len >= buf.length) {
//            socket.write(b, off, buf.length);
//            len -= buf.length;
//            off += buf.length;
//        }
//        System.arraycopy(b, off, buf, count, len);
//        count += len;
//    }
//
//    
//    /**
//     * {@inheritDoc}
//     */
//    
//    public synchronized void flush() throws IOException {
//        flushBuffer();
//    }
//    
//
//}