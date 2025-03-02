

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/*
  Proxy controller;
  ie the port number, cached/blocked sites  and turning the proxy server off and on

 */

public class Proxy implements Runnable {
    int portNumber = 4000; 
    public static ArrayList<String> blockedSites;   // Data structure to store blocked sites
    public static HashMap<String, byte[]> cachedSites; // Data structure for cached responses
    public static HashMap <String, long[]> timeData; 
    boolean isOn;
     String resetColour = "\u001B[0m";
     String cyan = "\u001B[36m";
     String red = "\u001B[31m";
    


    /*
     * Initializes the static lists for blocked sites and cache
     */
    Proxy(){
        blockedSites = new ArrayList<String>();        // stores domain names to block (e.g. "www.ebay.com")
        cachedSites = new HashMap<String, byte[]>();   // caches entire HTTP responses indexed by "GET|URL"
        timeData = new HashMap<String, long[]>();
        isOn = true;
    }

    private void blockSite(String domain){
        blockedSites.add(domain);
    }

    private void unblockSite(String domain){
        blockedSites.remove(domain);
    }

    private void stopProxy(){
        isOn = false;
    }


    private void timeData(){
        System.out.println("Printing out time data..(in ms)\n");
        System.out.println("Domain\t\t| Uncached\t\t| cached");
        for(String domain : timeData.keySet()){
            System.out.println(domain +"\t |" + (timeData.get(domain)[0]) / Math.pow(10, 6) + "\t\t  " + (timeData.get(domain)[1]) / Math.pow(10, 6) );
            
        }
    }

    private void helpMessage(){
       
        System.out.println( cyan + "\nWELCOME TO MY PROXY COMMAND CONSOLE\nhere are a few commands:"
         + resetColour + "\nHELP : for command list\nSTOP : to turn proxy off" + 
         "\nBLOCK <domain> : to block websites\nUNBLOCK <domain> : to unblock websites\n"
         + "TIMES : to see cached vs uncached times");
    }

    @Override
    public void run() {
        helpMessage();
        BufferedReader readTerminal = new BufferedReader(new InputStreamReader(System.in));
   
            String line;
        try {
            line = readTerminal.readLine();
            
            while((line != null) || !(line.isEmpty())){
                String [] commandSections = line.split(" ");
                String command = commandSections[0];
                String domain = (commandSections.length > 1)? commandSections[1]: null;


                switch(command){
                    case "STOP":
                    System.out.println("switching off web proxy server..");
                    stopProxy();
                    break;
                    case "HELP":
                    helpMessage();
                    break;
                    case "BLOCK":
                    System.out.println("Attempting to block domain:" + domain);
                    blockSite(domain);
                    break;
                    case "UNBLOCK":
                    System.out.println("Attempting to unblock domain:" + domain);
                    unblockSite(domain);
                    break;
                    case "TIMES":
                    timeData();
                    break;
                    default:
                    System.out.println(red + "INVALID COMMAND, please retry(commands are case sensitive)" + resetColour);
                    helpMessage();
                    break;
                }
                line = readTerminal.readLine();

            }
        } catch (IOException ex) {
        }
             
    }
    

}
