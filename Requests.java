import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;


/**
 * Handles individual client requests in its own thread
 */

public class Requests implements  Runnable{

    Socket client;    
    InputStreamReader cIn;
    BufferedReader cInReader;
    OutputStreamWriter      cOut;
    ArrayList <String> bannedSites;

    Requests(Socket client, ArrayList<String>bannedSites){
        this.client = client;
        this.bannedSites = bannedSites;

    }

    @Override
    public void run() {
      try {
        cIn = new InputStreamReader(client.getInputStream());
        cInReader = new BufferedReader(cIn);
        cOut = new OutputStreamWriter(client.getOutputStream());


        //processing request by first extracing url then obtaining request as string
        String line = cInReader.readLine(); //retrieve the request line
        String method = line.split(" ")[0];  //Http method ie get or connect
        ArrayList <String> req = new ArrayList<>();
        String current = line;

        //putting each line of the request into an arraylist
        while(current != null){
            req.add(current);
            current = cInReader.readLine();

        }

        String url = getUrl(line);
        System.out.println("current host is " + url);
        //System.out.println(req);
        if(!bannedSites.contains(url)){ 
          //if site from request not banned, make socket  to connect to webserver
          Socket server = new Socket(url, 80);
          if(method.equals("CONNECT")){
            httpSreq(server, req);

          }
          else{
            httpReq(server, req);
          }
          


          

          server.close();
          client.close();

        }
        else{//site at url banned
            cOut.write("Url banned");
            cOut.flush();


        }
        


      } catch (Exception e) {
        e.printStackTrace();

      }
      

      
    }

    //helper methods

    //Get Url
    //takes a request line as a parameter
    //ie CONNECT www.somewebsite.com:100 HTTP/1.1
    //splits it by spaces to take www.somewebsite.com:100 out
    //extracts string from start to before : to get just the link and not the port number
    private String getUrl(String reqLine){
        String url_and_port = reqLine.split(" ")[1];
        int portStartIndex = url_and_port.indexOf(':');
        String url = url_and_port.substring(0, portStartIndex);
        return url;
        
    }



    //handling http requests
    private void httpReq(Socket server, ArrayList<String> req) throws IOException{
      //write request to server
      BufferedWriter writeToServer = new BufferedWriter(
        new OutputStreamWriter(server.getOutputStream()));
        

        //for each line of the request, write 
        System.out.println("writing request out: ");
        for(String x : req){
          writeToServer.write(x);
          System.out.println(x);
        }
        writeToServer.flush();

      //read server's response
      BufferedReader readFromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
      String reply;
      while ((reply = readFromServer.readLine())!= null){
        cOut.write(reply + "\r\n");
        cOut.flush();
      }

    }

    private void httpSreq (Socket server, ArrayList<String> req) throws IOException{
      System.out.println("this is a https request woops");

    }


    
    
}
