

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

/**
 * Handles individual client requests in its own thread
 */
public class Requests implements Runnable {

    // The client socket that connected to our proxy
    private Socket client;

    // Streams for reading/writing data to/from client
    private InputStream fromClient;
    private OutputStream toClient;
    private BufferedReader fromClientR;
    private BufferedWriter toClientW;

    // pointer to blocked sites list
    private ArrayList<String> bannedSites;
    // pointer to cached sites list
    private HashMap<String, byte[]> cachedSites;

    private HashMap<String, long[]> times;



    /**
     * Constructor
     * 
     * @param client the socket connected to the client (browser)
     * @param bannedSites the shared list of blocked domains
     * @param cachedSites the shared map for caching HTTP GET responses
     */
    public Requests(Socket client, ArrayList<String> bannedSites, HashMap<String, byte[]> cachedSites, HashMap<String, long[]> times) {
        //initialising pointers to my client, sites list, cache list and timing hashmap
        this.client = client;
        this.bannedSites = bannedSites;
        this.cachedSites = cachedSites;
        this.times = times;
    }

    /**
     * Main entry point for this request-handling thread.
     */
    @Override
    public void run() {
        try {
            // Set up streams to read the client's request and send back a response
            fromClient = client.getInputStream();
            toClient   = client.getOutputStream();
            fromClientR = new BufferedReader(new InputStreamReader(fromClient));
            toClientW   = new BufferedWriter(new OutputStreamWriter(toClient));

            // Get the request line, e.g. "GET http://www.example.com/ HTTP/1.1"
            String line = fromClientR.readLine();
            if (line == null || line.isEmpty()) {
                // If invalid or empty, just return
                return;
            }

            // HTTP or HTTPS method (CONNECT, GET, POST, etc.)
            String method = line.split(" ")[0];

            // Collect the entire HTTP request into a list
            ArrayList<String> requestLines = new ArrayList<>();
            requestLines.add(line);

            // Read subsequent lines (headers) until an empty line
            String current;
            while ((current = fromClientR.readLine()) != null && !current.isEmpty()) {
                requestLines.add(current);
            }

            // Identify the host from the request line
            String host = getHost(line);
            // We'll also parse out just the domain to match our block list
            String domain = getDomain(line);

            // System.out.println("\nRequest line is: " + line);
            // System.out.println("Host is : " + host);
            // System.out.println("Domain is: " + domain);  

            if (host == null) {
                // If we couldn't parse a valid host, we can't proceed
                return;
            }

            // SITE BLOCKING:
            // If this domain is in bannedSites, respond with a simple "403 Forbidden"
            if (bannedSites.contains(domain)) {
                toClientW.write("HTTP/1.1 403 Forbidden\r\n");
                toClientW.write("Content-Type: text/plain\r\n");
                toClientW.write("\r\n");
                toClientW.write("This site is banned.\r\n");
                toClientW.flush();
                return; // no further forwarding to real server
            }
            else {
                // If not on block list, check if HTTPS or HTTP
                if ("CONNECT".equalsIgnoreCase(method)) {
                    // For HTTPS, pass to handleHTTPSRequest
                    handleHTTPSRequest(host);
                } else {
                    // For normal HTTP, handle request (with potential caching)
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
     * Helper method to extract the "host:port" portion from the request line.
     * 
     * If it's CONNECT, typically looks like: "CONNECT www.example.com:443 HTTP/1.1"
     * Otherwise might look like: "GET http://www.example.com/page HTTP/1.1"
     * 
     * @param reqLine The first line of the HTTP request
     * @return e.g. "www.example.com" or "www.example.com:443"
     */
    private String getHost(String reqLine){
        if (reqLine == null) return null;

        String[] parts = reqLine.split(" ");
        if (parts.length < 2) return null;
        String fullUrl = parts[1];  // e.g. "http://www.example.com/"

        // If method is CONNECT, the URL is typically just "www.example.com:443"
        if (reqLine.startsWith("CONNECT ")) {
            return fullUrl; 
        }
        else {
            // HTTP request lines often have "http://www.example.com/"
            if (fullUrl.startsWith("http://")) {
                fullUrl = fullUrl.substring(7); // remove "http://"
            }
            else if (fullUrl.startsWith("https://")) {
                fullUrl = fullUrl.substring(8); // remove "https://"
            }
            // Remove any path portion
            int slashIndex = fullUrl.indexOf('/');
            if (slashIndex >= 0) {
                fullUrl = fullUrl.substring(0, slashIndex);
            }
            return fullUrl;  // e.g. "www.example.com"
        }
    }

    /**
     * Extracts just the domain (e.g. "www.example.com") from a "host:port" string (e.g. "www.example.com:443")
     * This is used to check if it's in the bannedSites list.
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
     * Handle a standard HTTP request (non-CONNECT).
     * This includes a basic caching mechanism for GET requests.
     */
    private void handleHTTPRequest(String host, ArrayList<String> requestLines) throws IOException {


        //http request start time for timing data
        long startTime = System.nanoTime();
        // From the first request line, parse method & raw URL
        String firstLine = requestLines.get(0); 
        String domain = getHost(firstLine);
        String[] tokens  = firstLine.split(" ");
        String method    = tokens[0];   // e.g. "GET"
        String rawUrl    = tokens[1];   // e.g. "http://www.example.com/page"

        // Build a cache key, e.g. "GET|http://www.example.com/page"
        String cacheKey = method.toUpperCase() + "|" + rawUrl;

        // Check if it's a GET request and if the response is already cached
        if ("GET".equalsIgnoreCase(method) && cachedSites.containsKey(cacheKey)) {
           
            // Retrieve cached data (the entire HTTP response as a byte[])
            byte[] cachedResponse = cachedSites.get(cacheKey);

            // Send that data back to the client
            toClient.write(cachedResponse);

            //get endtime
            //if page has been cached, then its been accessed before, therefore a time data entry exists for the non cached request
            long endTime = System.nanoTime();
            System.out.println("stary time is " + startTime);
            System.out.println("END TIME IS " + endTime);
            times.get(domain)[1] = endTime - startTime;
            
            toClient.flush();
            return; // Done, no need to contact the origin server
        }

        // If not cached (or method is not GET), we must forward request to the real server:

        // If host includes a port, parse it out; otherwise default to 80 for HTTP
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
        // End of headers
        toServerB.write("\r\n");
        toServerB.flush();

        // Read the server's response fully
        InputStream fromServer = server.getInputStream();
        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = fromServer.read(buffer)) != -1) {
            responseBuffer.write(buffer, 0, bytesRead);
        }
        // Entire response is now in responseBuffer
        byte[] serverResponse = responseBuffer.toByteArray();

        // Send the server's response to the client
        BufferedOutputStream toClientB = new BufferedOutputStream(client.getOutputStream());
        long endTime = System.nanoTime();
        long[] timesArray = new long[2];
        timesArray[0] = endTime - startTime;
        times.put(domain, timesArray);
        toClientB.write(serverResponse);
        toClientB.flush();

        // If this was a GET request, store the response in the cache
        if ("GET".equalsIgnoreCase(method)) {
            cachedSites.put(cacheKey, serverResponse);
            //System.out.println("CACHE STORE for: " + cacheKey);
        }

        // Close resources
        toServerB.close();
        fromServer.close();
        toClientB.close();
        server.close();
    }

    /**
     * Handle an HTTPS (CONNECT) request by tunneling data directly (no caching).
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

            // Notify client that the connection is established
            BufferedWriter proxyToClientWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            proxyToClientWriter.write("HTTP/1.1 200 Connection Established\r\n");
            proxyToClientWriter.write("Proxy-Agent: MyProxy\r\n");
            proxyToClientWriter.write("\r\n");
            proxyToClientWriter.flush();

            // Set up bidirectional forwarding: client <--> proxy <--> server
            InputStream fromServer = server.getInputStream();
            OutputStream toServer  = server.getOutputStream();

            // Threads to forward data
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



