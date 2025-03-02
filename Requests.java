// import java.io.BufferedOutputStream;
// import java.io.BufferedReader;
// import java.io.BufferedWriter;
// import java.io.IOException;
// import java.io.InputStream;
// import java.io.InputStreamReader;
// import java.io.OutputStream;
// import java.io.OutputStreamWriter;
// import java.net.Socket;
// import java.util.ArrayList;


// /**
//  * Handles individual client requests in its own thread
//  */

// public class Requests implements  Runnable{

//     Socket client;    
//     InputStream fromClient;
//     OutputStream   toClient;
//     BufferedReader fromClientR;
//     BufferedWriter toClientW;
   
//     ArrayList <String> bannedSites;

//     Requests(Socket client, ArrayList<String>bannedSites){
//         this.client = client;
//         this.bannedSites = bannedSites;

//     }

//     @Override
//     public void run() {
//       try {
//         fromClient = client.getInputStream();
//         toClient = client.getOutputStream();
//         fromClientR = new BufferedReader(new InputStreamReader(fromClient));
//         toClientW = new BufferedWriter(new OutputStreamWriter(toClient));
        
//         //processing request by first extrafromClientg url then obtaining request as string
//         String line = fromClientR.readLine(); //retrieve the request line
        
//         System.out.println("line :" + line);
//         String method = line.split(" ")[0];  //Http method ie get or connect

//         System.out.println("\n\n\n");
//         System.out.println("line is :" + line);
//         System.out.println("method :" + method);
//         ArrayList <String> req = new ArrayList<>();
//         String current = line;

//         //putting each line of the request into an arraylist
//         while(!current.isEmpty()){
//             req.add(current);
//             current = fromClientR.readLine();

//         }

//         String host = getHost(line);
//         if(host == null ) return;
//         System.out.println("current host is " + host);
        
//         if(!bannedSites.contains(host)){ 
//           //if site from request not banned, check if HTTPS or HTTP request
//           if(method.equals("CONNECT")){ //if https request pass to https method

//             handleHTTPSRequest(host);
//           }
//           else{ // else pass to http method
//             httpReq(host, req);
//           }
          


          

    
//           client.close();

//         }
//         else{//site at url banned
//             toClientW.write("Url banned");
//             toClientW.flush();


//         }
        


//       } catch (Exception e) {
//         e.printStackTrace();

//       }
      

      
//     }

//     //helper methods

//     //get host
//     //if its a http request, removes specific port if its there
//     // to get host name

//     //if its a http request remove "http://" and afterwards remove specific pages of request ie
//     // if it was www.google.com/page/subpage remove everything after the first slash

//     private String getHost(String reqLine){


//         if (reqLine == null) return null; //if invalid request line 
//         String fullUrl = reqLine.split(" ")[1];

//         //extracting host from HTTPS requests like
//          //ie CONNECT www.somewebsite.com:100 HTTP/1.1
//         if(reqLine.contains("CONNECT")){
        
//           int portIndex = fullUrl.indexOf(":");
//           return (portIndex == - 1 )? fullUrl :fullUrl.substring(0, portIndex); // if it does contain the port output the host without it
//                                                                                            // otherwise send on host as is
//         }
//         //extracting host from HTTP requests like
//         //GET http://www.example.com/ HTTP/1.1
//         else{
//           int httpIndex = fullUrl.indexOf("http://");
//           String withOutHttp =  fullUrl.substring(httpIndex + 7, fullUrl.length());
//           System.out.println("withoutHttp is " + withOutHttp);
//           int pagesIndex = withOutHttp.indexOf("/"); //get first / and remove string from there to extract just host
//           return withOutHttp.substring(0, pagesIndex);
//         }

        
        
//     }

  




//   private void httpReq(String host, ArrayList<String> req) throws IOException {
//     Socket server = new Socket(host, 80);
//     BufferedWriter toServerB = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
//     BufferedOutputStream toClientB = new BufferedOutputStream(client.getOutputStream());

    
//     InputStream fromServer = server.getInputStream();
  
//     for (String line : req) {
//         toServerB.write(line + "\r\n");
//         System.out.println(line);
//     }

//     // Ensure the request ends properly
//     toServerB.write("Connection: close\r\n\r\n");
//     toServerB.flush();

//     // Read the response and forward it to the client
//     byte[] buffer = new byte[8192]; //8kb buffer
//     int bytesRead;
//     while ((bytesRead = fromServer.read(buffer)) != -1) {
//         toClientB.write(buffer, 0, bytesRead);
//         toClientB.flush();
//     }

//     // Closing streams and sockets
//     toServerB.close();
//     fromServer.close();
//     toClientB.close();
//     server.close();
//     client.close();
// }


// /*private void httpsreq(String host, ArrayList<String> req) throws IOException{

//         try {
//             int portNumber;
//             //get specific port number if listed, otherwise default to 443
//             if (req.get(1).contains(":")){
//                 String portNString = req.get(1).split(":")[1];
//                 portNumber = Integer.parseInt(portNString);}
//             else{
//                 portNumber = 443; //default for https
//             }

            
            
//             Socket server = new Socket(host, portNumber);
//             toClientW.write("HTTP/1.1 200 Connection Established\r\n\r\n");
//             toClientW.flush();
            
//             server.setSoTimeout(15000);
            
//             InputStream fromServer = (server.getInputStream());
//             OutputStream toServer = server.getOutputStream();
            
//             //thread to handle data being sent from client to server and vice versa
//             Thread clientToServer = new Thread(new HttpsTransfer(fromClient, toServer));
//             clientToServer.start();
            
//             Thread serverToClient = new Thread(new HttpsTransfer(fromServer, toClient));
//             serverToClient.start();
            
//             //wait for both threads to finish before ending request
//             clientToServer.join();
//             serverToClient.join();


//         } catch (InterruptedException ex) {
//         }



// }
// */
// private void httpsreq(String host, ArrayList<String> req) throws IOException {
//   try {
//       int portNumber = 443;

//       // Extract port number from request if specified
//       if (req.get(1).contains(":")) {
//           String portNString = req.get(1).split(":")[1];
//           portNumber = Integer.parseInt(portNString);
//       }

//       System.out.println("Connecting to " + host + " on port " + portNumber);
      
//       Socket server = new Socket(host, portNumber);
//       server.setSoTimeout(15000);

//       System.out.println("Connected to " + host);

//       // Send "200 Connection Established" to the client (browser)
//       BufferedWriter proxyResponse = new BufferedWriter(new OutputStreamWriter(toClient));
//       proxyResponse.write("HTTP/1.1 200 Connection Established\r\n");
//       proxyResponse.write("Proxy-Agent: MyProxy\r\n");
//       proxyResponse.write("\r\n");
//       proxyResponse.flush();

//       System.out.println("Sent 200 Connection Established to client");

//       InputStream fromServer = server.getInputStream();
//       OutputStream toServer = server.getOutputStream();

//       // Start forwarding data between client and server
//       Thread clientToServer = new Thread(new HttpsTransfer(fromClient, toServer));
//       Thread serverToClient = new Thread(new HttpsTransfer(fromServer, toClient));

//       clientToServer.start();
//       serverToClient.start();

//       clientToServer.join();
//       serverToClient.join();

//       System.out.println("HTTPS data transfer complete. Closing connections.");
//       server.close();
      

//   } catch (InterruptedException e) {
//       e.printStackTrace();
//   }
// }

// private void handleHTTPSRequest(String urlString) {
//     try {
//         // Extract hostname and port from CONNECT request
//         String url = urlString.substring(7);
//         String[] pieces = url.split(":");
//         url = pieces[0];
//         int port = (pieces.length > 1) ? Integer.parseInt(pieces[1]) : 443;

//         System.out.println("Attempting HTTPS connection to " + url + ":" + port);

//         // Resolve IP address and open connection to target server
//         //InetAddress address = InetAddress.getByName(url);
//         Socket proxyToServerSocket = new Socket(url, port);

//         System.out.println("Connected to " + url + ":" + port);

//         // Send "200 Connection Established" response to client
//         BufferedWriter proxyToClientWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
//         proxyToClientWriter.write("HTTP/1.1 200 Connection Established\r\n");
//         proxyToClientWriter.write("Proxy-Agent: ProxyServer/1.0\r\n");
//         proxyToClientWriter.write("\r\n");
//         proxyToClientWriter.flush();

//         System.out.println("Sent 200 Connection Established to client");

//         // Get input/output streams
//         // InputStream clientInput = client.getInputStream();
//         // OutputStream clientOutput = client.getOutputStream();
//         InputStream serverInput = proxyToServerSocket.getInputStream();
//         OutputStream serverOutput = proxyToServerSocket.getOutputStream();

//         // Create threads to forward data bidirectionally
//         Thread clientToServerThread = new Thread(new DataForwarder(fromClient, serverOutput));
//         Thread serverToClientThread = new Thread(new DataForwarder(serverInput, toClient));

//         clientToServerThread.start();
//         serverToClientThread.start();

//         // Wait for data transfer to complete
//         clientToServerThread.join();
//         serverToClientThread.join();

//         // Close sockets
//         proxyToServerSocket.close();
//         client.close();

//         System.out.println("HTTPS tunnel closed");

//     } catch (Exception e) {
//         e.printStackTrace();
//     }
// }


// /*private void handleHTTPSRequest(String urlString) {
//   try {
//       // Extract hostname and port from CONNECT request
//       String url = urlString.substring(7);
//       String[] pieces = url.split(":");
//       url = pieces[0];
//       int port = (pieces.length > 1) ? Integer.parseInt(pieces[1]) : 443;

//       System.out.println("Attempting HTTPS connection to " + url + ":" + port);

//       // Resolve IP address and open connection to target server
//       InetAddress address = InetAddress.getByName(url);
//       Socket proxyToServerSocket = new Socket(address, port);

//       System.out.println("Connected to " + url + ":" + port);

//       // Send "200 Connection Established" response to client
//       BufferedWriter proxyToClientWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
//       proxyToClientWriter.write("HTTP/1.1 200 Connection Established\r\n");
//       proxyToClientWriter.write("Proxy-Agent: ProxyServer/1.0\r\n");
//       proxyToClientWriter.write("Connection: keep-alive\r\n");
//       proxyToClientWriter.write("\r\n");
//       proxyToClientWriter.flush();

//       System.out.println("Sent 200 Connection Established to client");

//       // Get input/output streams
//       InputStream clientInput = client.getInputStream();
//       OutputStream clientOutput = client.getOutputStream();
//       InputStream serverInput = proxyToServerSocket.getInputStream();
//       OutputStream serverOutput = proxyToServerSocket.getOutputStream();

//       // Forward data bidirectionally
//       Thread clientToServerThread = new Thread(new DataForwarder(clientInput, serverOutput));
//       Thread serverToClientThread = new Thread(new DataForwarder(serverInput, clientOutput));

//       clientToServerThread.start();
//       serverToClientThread.start();

//       // Wait for data transfer to complete
//       clientToServerThread.join();
//       serverToClientThread.join();

//       System.out.println("HTTPS tunnel closed");

//   } catch (Exception e) {
//       e.printStackTrace();
//   }
// }
// */


// /*private void httpsReq(String host, ArrayList<String> req) {
//   try {
//       // Extract hostname and port from CONNECT request
//       String url = urlString.substring(7);
//       String[] parts = url.split(":");
//       String host = parts[0];
//       int port = (parts.length > 1) ? Integer.parseInt(parts[1]) : 443;

//       InetAddress address = InetAddress.getByName(host);
//       Socket proxyToServerSocket = new Socket(address, port);
//       proxyToServerSocket.setSoTimeout(20000);

//       // Send "200 Connection Established" response to client
//       BufferedWriter proxyToClientWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
//       proxyToClientWriter.write("HTTP/1.1 200 Connection Established\r\n");
//       proxyToClientWriter.write("Proxy-Agent: ProxyServer/1.0\r\n");
//       proxyToClientWriter.write("Connection: keep-alive\r\n");
//       proxyToClientWriter.write("\r\n");
//       proxyToClientWriter.flush();

//       System.out.println("Sent 200 Connection Established to client");

//       // Set up bidirectional data transfer
//       InputStream clientInput = client.getInputStream();
//       OutputStream clientOutput = client.getOutputStream();
//       InputStream serverInput = proxyToServerSocket.getInputStream();
//       OutputStream serverOutput = proxyToServerSocket.getOutputStream();

//       Thread clientToServerThread = new Thread(new DataForwarder(clientInput, serverOutput));
//       Thread serverToClientThread = new Thread(new DataForwarder(serverInput, clientOutput));

//       clientToServerThread.start();
//       serverToClientThread.start();

//       // Keep tunnel open for multiple requests
//       clientToServerThread.join();
//       serverToClientThread.join();

//       proxyToServerSocket.close();
//       client.close();

//   } catch (Exception e) {
//       e.printStackTrace();
//   }
// }
// */

  


    
    
// }
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Handles individual client requests in its own thread
 */
public class Requests implements Runnable {

    private Socket client;
    private InputStream fromClient;
    private OutputStream toClient;
    private BufferedReader fromClientR;
    private BufferedWriter toClientW;

    private ArrayList<String> bannedSites;

    public Requests(Socket client, ArrayList<String> bannedSites) {
        this.client = client;
        this.bannedSites = bannedSites;
    }

    @Override
    public void run() {
        try {
            fromClient = client.getInputStream();
            toClient = client.getOutputStream();
            fromClientR = new BufferedReader(new InputStreamReader(fromClient));
            toClientW = new BufferedWriter(new OutputStreamWriter(toClient));

            // getting request line
            String line = fromClientR.readLine();
            if (line == null || line.isEmpty()) {
                return;
                //client closed in finally block;
            }

          
            //Http or https method like CONNECT or GET
            String method = line.split(" ")[0];
            // Collect the entire HTTP request into array list
            ArrayList<String> requestLines = new ArrayList<>();
            requestLines.add(line);
            String current;
            while ((current = fromClientR.readLine()) != null && !current.isEmpty()) {
                requestLines.add(current);
            }

            //Get host from request line
            String host = getHost(line);
            String domain = getDomain(line);
            System.out.println("\nreq line is " + line);
            System.out.println("host is :" + host);
            System.out.println("domain is " + domain);  
            
            if (host == null) {
                return;
            }
            //System.out.println("Host extracted: " + host);

            // If site is banned, respond with an error ///
            if (bannedSites.contains(domain)) {
                toClientW.write("HTTP/1.1 403 Forbidden\r\n");
                toClientW.write("Content-Type: text/plain\r\n");
                toClientW.write("\r\n");
                toClientW.write("This site is banned.\r\n");
                toClientW.flush();
                //client closed in finally
                return;
            }
            else{
            // If nnot on block list check if http or https
            if ("CONNECT".equalsIgnoreCase(method)) {
                handleHTTPSRequest(host);
            } else {
                handleHTTPRequest(host, requestLines);
            }
          }

        } catch (Exception e) {
            e.printStackTrace();
        } finally { //irregardless of whether or not the connection succeeds, close the opened client socket
            try {
              client.close();
            } catch (IOException e) {
             
              e.printStackTrace();
            }
        }
    }

   

     /*Get host
      Param reqLine: the first line of a request 
      *  takes out the host
      */
    private String getHost(String reqLine) {
        if (reqLine == null) {
            return null;
        }

        String[] reqLineArray = reqLine.split(" ");
        if (reqLineArray.length < 2) {
            //if it doesnt have a http/https method and a host, return nuthin
            return null;
        }
        String url = reqLineArray[1]; //get host and port part

        if (reqLine.startsWith("CONNECT ")) {
            return url; // return host and port!
        } else {
            //For HTTP requests if theres a http:// at the start we get rid of it
            if (url.startsWith("http://")) {
                url = url.substring(7);
            }
          
            // If theres right slashes left in the string after http is removed 
            //
            int slashIndex = url.indexOf('/');
            if (slashIndex >= 0) {
                url = url.substring(0, slashIndex);
            }
            return url; 
        }
    }

    private String getDomain(String reqLine){
      String host = getHost(reqLine);
      
      return (host.contains(":"))?  host.substring(0,host.indexOf(':')): host;


    }

    /**
     * Handle a standard HTTP request.
     */
    private void handleHTTPRequest(String host, ArrayList<String> requestLines) throws IOException {
        // By default, port 80 for HTTP (unless user typed something like "www.example.com:8080")
        String hostname = host;
        int port = 80;
        int colonIndex = host.indexOf(':');
        if (colonIndex >= 0) {
            hostname = host.substring(0, colonIndex);
            port = Integer.parseInt(host.substring(colonIndex + 1));
        }

        Socket server = new Socket(hostname, port);

        // Forward the original request to the real server
        BufferedWriter toServerB = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
        for (String line : requestLines) {
            toServerB.write(line + "\r\n");
           // System.out.println("[HTTP->SERVER] " + line);
        }
        // End of headers
        toServerB.write("\r\n");
        toServerB.flush();

        // Now forward server response to the client
        BufferedOutputStream toClientB = new BufferedOutputStream(client.getOutputStream());
        InputStream fromServer = server.getInputStream();

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = fromServer.read(buffer)) != -1) {
            toClientB.write(buffer, 0, bytesRead);
            toClientB.flush();
        }

        // Close everything
        toServerB.close();
        fromServer.close();
        toClientB.close();
        server.close();
    }

    /**
     * Handle an HTTPS (CONNECT) request by tunneling data directly.
     */
    private void handleHTTPSRequest(String hostAndPort) {
        try {
            // seperate host from port number if port number present, otherwise default to 443 for https
            String hostname = hostAndPort;
            int port = 443;
            int colonIndex = hostAndPort.indexOf(':');
            if (colonIndex >= 0) {
                hostname = hostAndPort.substring(0, colonIndex);
                port = Integer.parseInt(hostAndPort.substring(colonIndex + 1));
            }

            // Connect to the remote server
            Socket server = new Socket(hostname, port);
            System.out.println("Connected to " + hostname + ":" + port + " for HTTPS");

            // Notify the client that the connection is established
            BufferedWriter proxyToClientWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            proxyToClientWriter.write("HTTP/1.1 200 Connection Established\r\n");
            proxyToClientWriter.write("Proxy-Agent: MyProxy\r\n");
            proxyToClientWriter.write("\r\n");
            proxyToClientWriter.flush();

            // Set up bidirectional forwarding: client <--> proxy <--> server
            InputStream fromServer = server.getInputStream();
            OutputStream toServer = server.getOutputStream();

            // Threads to forward data
            Thread clientToServer = new Thread(new DataForwarder(fromClient, toServer));
            Thread serverToClient = new Thread(new DataForwarder(fromServer, toClient));

            // Start the forwarding
            clientToServer.start();
            serverToClient.start();

            // Wait for them to finish
            clientToServer.join();
            serverToClient.join();

            // Close everything
            server.close();
            System.out.println("HTTPS tunnel for " + hostname + ":" + port + " closed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   
}


