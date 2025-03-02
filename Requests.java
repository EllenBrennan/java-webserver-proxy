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

public class Requests implements  Runnable{

    Socket client;    
    InputStream fromClient;
    OutputStream   toClient;
    BufferedReader fromClientR;
    BufferedWriter toClientW;
   
    ArrayList <String> bannedSites;

    Requests(Socket client, ArrayList<String>bannedSites){
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
        
        //processing request by first extrafromClientg url then obtaining request as string
        String line = fromClientR.readLine(); //retrieve the request line
        
        System.out.println("line :" + line);
        String method = line.split(" ")[0];  //Http method ie get or connect

        System.out.println("\n\n\n");
        System.out.println("line is :" + line);
        System.out.println("method :" + method);
        ArrayList <String> req = new ArrayList<>();
        String current = line;

        //putting each line of the request into an arraylist
        while(!current.isEmpty()){
            req.add(current);
            current = fromClientR.readLine();

        }

        String host = getHost(line);
        if(host == null ) return;
        System.out.println("current host is " + host);
        
        if(!bannedSites.contains(host)){ 
          //if site from request not banned, check if HTTPS or HTTP request
          if(method.equals("CONNECT")){ //if https request pass to https method

            //httpSreq(server, req); 
          }
          else{ // else pass to http method
            httpReq(host, req);
          }
          


          

    
          client.close();

        }
        else{//site at url banned
            toClientW.write("Url banned");
            toClientW.flush();


        }
        


      } catch (Exception e) {
        e.printStackTrace();

      }
      

      
    }

    //helper methods

    //get host
    //if its a http request, removes specific port if its there
    // to get host name

    //if its a http request remove "http://" and afterwards remove specific pages of request ie
    // if it was www.google.com/page/subpage remove everything after the first slash

    private String getHost(String reqLine){


        if (reqLine == null) return null; //if invalid request line 
        String fullUrl = reqLine.split(" ")[1];

        //extracting host from HTTPS requests like
         //ie CONNECT www.somewebsite.com:100 HTTP/1.1
        if(reqLine.contains("CONNECT")){
          int portIndex = fullUrl.indexOf(":");
          return fullUrl.substring(0, portIndex);
        }
        //extracting host from HTTP requests like
        //GET http://www.example.com/ HTTP/1.1
        else{
          int httpIndex = fullUrl.indexOf("http://");
          String withOutHttp =  fullUrl.substring(httpIndex + 7, fullUrl.length());
          System.out.println("withoutHttp is " + withOutHttp);
          int pagesIndex = withOutHttp.indexOf("/"); //get first / and remove string from there to extract just host
          return withOutHttp.substring(0, pagesIndex);
        }

        
        
    }

  




  private void httpReq(String host, ArrayList<String> req) throws IOException {
    Socket server = new Socket(host, 80);
    BufferedWriter toServerB = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
    BufferedOutputStream toClientB = new BufferedOutputStream(client.getOutputStream());

    
    InputStream fromServer = server.getInputStream();
  
    for (String line : req) {
        toServerB.write(line + "\r\n");
        System.out.println(line);
    }

    // Ensure the request ends properly
    toServerB.write("Connection: close\r\n\r\n");
    toServerB.flush();

    // Read the response and forward it to the client
    byte[] buffer = new byte[8192]; //8kb buffer
    int bytesRead;
    while ((bytesRead = fromServer.read(buffer)) != -1) {
        toClientB.write(buffer, 0, bytesRead);
        toClientB.flush();
    }

    // Closing streams and sockets
    toServerB.close();
    fromServer.close();
    toClientB.close();
    server.close();
    client.close();
}


private void httpsreq(String host, ArrayList<String> req) throws IOException{

        try {
            int portNumber;
            //get specific port number if listed, otherwise default to 443
            if (req.get(1).contains(":")){
                String portNString = req.get(1).split(":")[1];
                portNumber = Integer.parseInt(portNString);}
            else{
                portNumber = 443; //default for https
            }

            
            
            Socket server = new Socket(host, portNumber);
            
            server.setSoTimeout(15000);
            
            InputStream fromServer = (server.getInputStream());
            OutputStream toServer = server.getOutputStream();
            
            //thread to handle data being sent from client to server and vice versa
            Thread clientToServer = new Thread(new HttpsTransfer(fromClient, toServer));
            clientToServer.start();
            
            Thread serverToClient = new Thread(new HttpsTransfer(fromServer, toClient));
            serverToClient.start();
            
            //wait for both threads to finish before ending request
            clientToServer.join();
            serverToClient.join();


        } catch (InterruptedException ex) {
        }



}


  


    
    
}
