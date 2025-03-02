

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

    /** Requests class handles individual client requests on their own thead
      @param  client: the socket connecting the proxy server port and the client ie browser request
      @param bannedSites: a pointer to the banned sites list
      @param cachedSiteS: a pointer to the cache hashmap
      @param times:  a pointer to the time data hashmap for 
     **/

public class Requests implements Runnable {

    private Socket client;

    private InputStream fromClient;
    private OutputStream toClient;
    private BufferedReader fromClientR;
    private BufferedWriter toClientW;
    private ArrayList<String> bannedSites;
    private HashMap<String, byte[]> cachedSites;
    private HashMap<String, long[]> times;



    public Requests(Socket client, ArrayList<String> bannedSites, HashMap<String, byte[]> cachedSites, HashMap<String, long[]> times) {
        //initialising pointers to my client, sites list, cache list and timing hashmap
        this.client = client;
        this.bannedSites = bannedSites;
        this.cachedSites = cachedSites;
        this.times = times;
    }

    /**
     *  RUN
     *  client request handled on each thread
     */
    @Override
    public void run() {
        try {
            // Setting up streams for writing to/reading from client
            fromClient = client.getInputStream();
            toClient   = client.getOutputStream();
            fromClientR = new BufferedReader(new InputStreamReader(fromClient));
            toClientW   = new BufferedWriter(new OutputStreamWriter(toClient));

            //Getting the first line of tjhe request
            String line = fromClientR.readLine();
            if (line == null || line.isEmpty()) {   // If invalid or empty, just return, closing client socket happens at the end anyways.
                return;
            }
            // the first thing in the request line should be the method ie GET or CONNECT  
            String method = line.split(" ")[0];

            //Collecting each line of the request into an arraylist 
            ArrayList<String> requestLines = new ArrayList<>();
            requestLines.add(line);
            String current;
            while ((current = fromClientR.readLine()) != null && !current.isEmpty()) {
                requestLines.add(current);
            }

            // Identify the host from the request line(host includes the port), we also get the domain(host w/o the port)
            String host = getHost(line);
            String domain = getDomain(line);

            if (host == null) {
                // If we couldn't parse a valid host, we can't proceed so we leave and close the client socket at finally
                return;
            }

            // SITE BLOCKING:
            // if the websites domain is present in our ban list we can't proceed so we don't send the request on to the server
            if (bannedSites.contains(domain)) {
                toClientW.write("HTTP/1.1 403 Forbidden\r\n");
                toClientW.write("Content-Type: text/plain\r\n");
                toClientW.write("\r\n");
                toClientW.flush();
                return; // no further forwarding to real server
            }
            else {
                // If not on block list, check if HTTPS or HTTP
                if ("CONNECT".equalsIgnoreCase(method)) {//HTTPS so pass host to https handler function
                    handleHTTPSRequest(host);
                } else {
                    //otherwise http handle with http handler method
                    handleHTTPRequest(host, requestLines);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Regardless of what happens, close the client socket
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method to take the "host:port" part of the request line out
     * @param reqLine: the first line of the http/https request */
    private String getHost(String reqLine){
        if (reqLine == null) return null; //if empty request line return

        String[] parts = reqLine.split(" "); //split request line by spaces
        if (parts.length < 2) return null;      // if there are less then two sections two this line ie just CONNECT instead of CONNECT www.website.com, return
        String fullUrl = parts[1];

        // If method is CONNECT we just return the whole url for https
        if (reqLine.startsWith("CONNECT ")) {
            return fullUrl; 
        }
        else {
            // HTTP request lines often have "http://" in the front so we're gonna remove that here
            if (fullUrl.startsWith("http://")) {
                fullUrl = fullUrl.substring(7); // remove "http://"
            }
            // Remove additional paths added onto the end of the url, we just want the domain and port(if its included)
            int slashIndex = fullUrl.indexOf('/');
            if (slashIndex >= 0) {
                fullUrl = fullUrl.substring(0, slashIndex);
            }
            return fullUrl;  // e.g. "www.example.com"
        }
    }




    /**
     * Extracts just the domain (e.g. "www.example.com") from a host thats formatted like host:port
     * if it doesnt have port stuff on it then it just returns the host
     */
    private String getDomain(String reqLine){
        String host = getHost(reqLine);
        if (host == null) return null;

        // If there's a colon, remove ":port"
        if (host.contains(":")){
            return host.substring(0, host.indexOf(':'));
        }
        return host; 
    }

    /**
     * Handle Http request
    */
    private void handleHTTPRequest(String host, ArrayList<String> requestLines) throws IOException {
        //tracking start time to see how long it takes to fufill the http request
        long startTime = System.nanoTime();

        // From the first request line get method and url
        String firstLine = requestLines.get(0); 
        String domain = getHost(firstLine);
        String[] sections  = firstLine.split(" ");
        String method    = sections[0];   
        String url    = sections[1];   

        // our key is just the request type and the specific url
        String key = method.toUpperCase() + " " + url;

        // if we have a GET http request check if page is stored within our cache
        if ("GET".equals(method) && cachedSites.containsKey(key)) {
           
            //retrieve stored byte array of from cache
            byte[] cachedResponse = cachedSites.get(key);

            //Send that data to client
            toClient.write(cachedResponse);

            
            //if page has been cached, then its been accessed before, therefore a time data entry exists for the non cached request
            //we plug the time elapsed since this method was called into the timedata hashmap
            long endTime = System.nanoTime();
            System.out.println("stary time is " + startTime);
            System.out.println("END TIME IS " + endTime);
            times.get(domain)[1] = endTime - startTime;
            
            toClient.flush();
            return; // Done, no need to contact the origin server
        }

        // If not cached (or method is not GET), we must forward request to the real server:

        // If host includes a port we take it out and we make the socket connect to that port on the webserver
        //otherwise we default to port 80 for HTTP
        String hostname = host;
        int port = 80;
        int colonIndex = host.indexOf(':');
        if (colonIndex >= 0) {
            hostname = host.substring(0, colonIndex);
            port = Integer.parseInt(host.substring(colonIndex + 1));
        }

        // Connect to the origin server
        Socket server = new Socket(hostname, port);

        // Forward the original request lines to the server
        BufferedWriter toServerB = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
        for (String line : requestLines) {
            toServerB.write(line + "\r\n");
        }
        toServerB.write("\r\n");
        toServerB.flush();

        // reading in server response
        //we get byte inputstream from the server and read the contents of this stream in roughly 8kb chunks 
        InputStream fromServer = server.getInputStream();
        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream(); //used as a 
        //resizable byte array where I add each 8kb chunk of the response to
        byte[] buffer = new byte[8192]; 
        int bytesRead;
        while ((bytesRead = fromServer.read(buffer)) != -1) {//while theres still more data to read in, add to responseBufer
            responseBuffer.write(buffer, 0, bytesRead);
        }
        // once im finished writing to response buffer, plug into byte array
        byte[] serverResponse = responseBuffer.toByteArray();

        // Send response to client 
        BufferedOutputStream toClientB = new BufferedOutputStream(client.getOutputStream());
        toClientB.write(serverResponse);
        toClientB.flush();

        //get end time of uncached http request, use to calculate the total time elapsed
        
        long endTime = System.nanoTime();
        
        // If it was a GET request, store the page in cached pages
        //generate an entry in the timesdata hashmap for the current page
        if ("GET".equalsIgnoreCase(method)) {

            long[] timesArray = new long[2];
            timesArray[0] = endTime - startTime;
            times.put(domain, timesArray);
            cachedSites.put(key, serverResponse);
        }

        // closing streams and socket connection with webserver
        toServerB.close();
        fromServer.close();
        toClientB.close();
        server.close();
    }

    /**
     * handle https request
     */
    private void handleHTTPSRequest(String hostAndPort) {
        try {
            // Separate host from port (default 443 for HTTPS)
            String hostname = hostAndPort;
            int port = 443;
            int colonIndex = hostAndPort.indexOf(':');
            if (colonIndex >= 0) {
                hostname = hostAndPort.substring(0, colonIndex);
                port = Integer.parseInt(hostAndPort.substring(colonIndex + 1));
            }

            // Connect to the remote server
            Socket server = new Socket(hostname, port);
            //System.out.println("Connected to " + hostname + ":" + port + " for HTTPS");

            // connection established message
            //lets client know connection was a success
            BufferedWriter proxyToClientWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            proxyToClientWriter.write("HTTP/1.1 200 Connection Established\r\n");
            proxyToClientWriter.write("Proxy-Agent: MyProxy\r\n");
            proxyToClientWriter.write("\r\n");
            proxyToClientWriter.flush();

            //streams from/to remote server
            InputStream fromServer = server.getInputStream();
            OutputStream toServer  = server.getOutputStream();

            // Threads to forward data from the client to the server and vice versa
            Thread clientToServer = new Thread(new DataForwarder(fromClient, toServer));
            Thread serverToClient = new Thread(new DataForwarder(fromServer, toClient));

            // Start forwarding
            clientToServer.start();
            serverToClient.start();

            // Wait for them to finish
            clientToServer.join();
            serverToClient.join();

            // Close everything
            server.close();
           // System.out.println("HTTPS tunnel for " + hostname + ":" + port + " closed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}



