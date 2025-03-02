import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HttpsTransfer implements Runnable  {
    private InputStream from;
    private OutputStream to;

    public HttpsTransfer(InputStream from, OutputStream to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[8192];  
            int read;
            
            while ((read = from.read(buffer)) >= 0) {
                to.write(buffer, 0, read); 
                to.flush(); 
            }
        } catch (IOException e) {
            e.printStackTrace();  
      
        }

        try {
            from.close();
            to.close();
        } catch (Exception e) {
        }
               
           
        
    }
}
