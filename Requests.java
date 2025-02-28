import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;


/**
 * Handles individual client requests n their own thread
 */

public class Requests implements  Runnable{

    Socket client;
    InputStreamReader cIn;
    BufferedReader cInReader;

    Requests(Socket client){
        this.client = client;

    }

    @Override
    public void run() {
      try {
        cIn = new InputStreamReader(client.getInputStream());
        cInReader = new BufferedReader(cIn);
        System.out.println(cInReader.readLine());



      } catch (Exception e) {

      }
      

      
    }

    
    
}
