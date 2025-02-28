
import java.net.ServerSocket;
import java.net.Socket;
/*
Opens a socket listening on portNumber 
 */

public class Proxy {
    static int portNumber = 4000; 

    public static void main(String[] args) {
        try {
            //listening out on for client requests at port number 
            ServerSocket server = new ServerSocket(portNumber);
            boolean isOn = true; // will be controlled by console later

            while(isOn){
                Socket client = server.accept();
                new Thread(new Requests(client)).start(); //open request on individual thread
                
            }
            server.close(); //once isOn false close server
            
        } catch (Exception e) {
        }
        
    }


}
