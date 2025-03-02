import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


// Helper class for forwarding data in a separate thread
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
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
                to.flush();
            }
        } catch (IOException e) {
            // Often thrown when the client or server closes the connection
        } finally {
            try { from.close(); } catch (IOException e) { /* ignore */ }
            try { from.close(); } catch (IOException e) { /* ignore */ }
        }
    }
}
