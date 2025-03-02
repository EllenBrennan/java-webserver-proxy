import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// /*class DataForwarder implements Runnable {
//     private InputStream input;
//     private OutputStream output;

//     public DataForwarder(InputStream input, OutputStream output) {
//         this.input = input;
//         this.output = output;
//     }

//     @Override
//     public void run() {
//         try {
//             byte[] buffer = new byte[8192];
//             int bytesRead;
//             while ((bytesRead = input.read(buffer)) != -1) {
//                 output.write(buffer, 0, bytesRead);
//                 output.flush();
//             }
//         } catch (IOException e) {
//             // Silently handle socket closure errors
//         } finally {
//             try {
//                 input.close();
//                 output.close();
//             } catch (IOException e) {
//                 e.printStackTrace();
//             }
//         }
//     }
// }
// */

// class DataForwarder implements Runnable {
//     private InputStream input;
//     private OutputStream output;

//     public DataForwarder(InputStream input, OutputStream output) {
//         this.input = input;
//         this.output = output;
//     }

//     @Override
//     public void run() {
//         try {
//             byte[] buffer = new byte[8192];
//             int bytesRead;
//             while ((bytesRead = input.read(buffer)) != -1) {
//                 output.write(buffer, 0, bytesRead);
//                 output.flush();
//             }
//         } catch (IOException e) {
//             // Handle connection closures silently
//         } finally {
//             try {
//                 input.close();
//                 output.close();
//             } catch (IOException e) {
//                 e.printStackTrace();
//             }
//         }
//     }
// }


// Helper class for forwarding data in a separate thread
class DataForwarder implements Runnable {
    private InputStream input;
    private OutputStream output;

    public DataForwarder(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (IOException e) {
            // Often thrown when the client or server closes the connection
        } finally {
            try { input.close(); } catch (IOException e) { /* ignore */ }
            try { output.close(); } catch (IOException e) { /* ignore */ }
        }
    }
}
