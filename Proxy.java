
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
/*
Opens a socket listening on portNumber 
 */

public class Proxy {
    int portNumber = 4000; 
    public static ArrayList<String> blockedSites;
    public static HashMap <String, byte[]> cachedSites;

    Proxy(){
        blockedSites = new ArrayList<String>();
        cachedSites = new HashMap<>();
    }
    public static void main(String[] args) {
        try {
            
            Proxy myProxy = new Proxy();
            
            
            //listening out on for client requests at port number
            ServerSocket server = new ServerSocket(myProxy.portNumber);
            myProxy.blockedSites.add("www.ebay.com");
            boolean isOn = true; // will be controlled by console later
        

            while(isOn){
                Socket client = server.accept();
                Thread currentRequest = new Thread(new Requests(client, myProxy.blockedSites)); //open request on individual thread 
                currentRequest.start();
                
                
            }
            server.close(); //once isOn false close server
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }


}
