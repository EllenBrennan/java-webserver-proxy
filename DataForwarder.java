import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


// Helper class for forwarding data in a separate thread
// takes an input stream from the socket sending on data and an output stream from the socker receiving the data
class DataForwarder implements Runnable {
    private InputStream from;
    private OutputStream to;

    public DataForwarder(InputStream from, OutputStream to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[8192]; // write data to receiver in 8kb ish chunks 
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) { //while there's still data to be read from the input stream, we write
                                                            //an 8kb chunk of data to the receiver via buffer
                to.write(buffer, 0, bytesRead);
                to.flush();
            }
        } catch (IOException e) {
            // Often thrown when the client or server closes the connection
        } finally { //once the data is finished being transmitted, finally ensures that we close the input and output streams
            try { from.close(); 
                  to.close();
            } catch (IOException e) { /* ignore */ }
        
        }
    }
}
