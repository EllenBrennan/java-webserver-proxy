import java.net.ServerSocket;
import java.net.Socket;

public class main {
    public static void main(String[] args) {
        try {
            // Create instance of Proxy controller console
            Proxy myProxy = new Proxy();
            Thread console = new Thread(myProxy);
            console.start();
            
            // listening for client requests on portNumber
            ServerSocket server = new ServerSocket(myProxy.portNumber);
            System.out.println("Proxy listening on port " + myProxy.portNumber);

            // looping to accept connections
            while(myProxy.isOn){
                // Accept an incoming client connection
                Socket client = server.accept();
                // new thread made to handle each client request, passing on pointer to blocked sites and cached sites
                Thread currentRequest = new Thread(
                    new Requests(client, myProxy.blockedSites, myProxy.cachedSites, myProxy.timeData)
                );
                currentRequest.start();
            }

            // once isOn is false, close the server
            server.close(); 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}
