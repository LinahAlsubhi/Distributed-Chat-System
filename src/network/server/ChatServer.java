/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;
/*
 * CPCS 371 FINAL PROJECT
 * GROUP NO. :5
 * SECTION: EAR
 *
 * ChatServer.java
 *
 * This class implements the server side of a simple distributed chat system
 * using TCP sockets. The server:
 *  - Listens on a fixed TCP port (5000)
 *  - Accepts multiple client connections
 *  - Creates a dedicated handler thread (ClientHandler) per client
 *  - Broadcasts messages to all connected clients
 *  - Logs all important events and messages to a log file (log.txt)
 *
 * The server works together with:
 *  - ChatClient (client program)
 *  - ClientHandler (per-client thread logic)
 *
 * References:
 * [1] Myles, A. (2001). Java TCP Sockets and Swing Tutorial.https://www.ashishmyles.com/tutorials/tcpchat/index.html
 * [2] Threads and Multiprocessing CH 12: https://math.hws.edu/javanotes/c12/index.html
 * [3] Schildt, H. ,JAVA: The Complete Reference, 9th ed CH 11: https://www.sietk.org/downloads/javabook.pdf
 * [4] RendoxThread, D., Runnable, Callable, ExecutorService, and Future - all the ways to create threads in Java: https://dev.to/danielrendox/thread-runnable-callable-executorservice-and-future-all-the-ways-to-create-threads-in-java-2o86
 * [5] Java Date and Time: https://www.w3schools.com/java/java_date.asp
 * [6] Class Socket: https://docs.oracle.com/javase/8/docs/api/java/net/Socket.html
 * [7] Different ways of Reading a text file in Java: https://www.geeksforgeeks.org/java/different-ways-reading-text-file-java/
 *
 */

import java.net.*;
import java.util.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ChatServer {
    
    /* Name of the log file used to store all server events and messages. */
    static final String LOG_FILE = "log.txt";
    
     /*
     * Shared list of all connected clients.
     * Each ClientHandler in this list corresponds to one connected ChatClient.
     * Access to this list is synchronized when broadcasting.
     */
    static ArrayList<ClientHandler> ClientList = new ArrayList<>();     //List to store all connected clients.
    
       
     //Date time format used for timestamps in the log file.
    private static final DateTimeFormatter DATES_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");    //Time stamp format.[5]

        /*
     * the main method of the ChatServer.
     *
     * Responsibilities:
     *  1. Initialize and clear the log file when starting the server program. 
     *  2. Log server start event.
     *  3. Open a ServerSocket listening on port 5000.
     *  4. Accept incoming client connections in an infinite loop.
     *  5. For each client:
     *      - Log connection
     *      - Create a ClientHandler thread
     *      - Add handler to ClientList
     *      - Start the handler thread
     *
     * @param argv Command-line arguments 
     */
 public static void main(String argv[]) throws Exception { 
     // Clear the log file at server startup
        try {
         // Opening a PrintWriter without append and closing it immediately
        // to clear the file if it exists.
        new PrintWriter(LOG_FILE).close();
        
    } catch (FileNotFoundException e) {
        // If log file cannot be created or accessed, we ignore here.
        // Logging will fail later and print a message to console.
    }
        
        // Store the first log entry indicating server startup.
        ClientHandler.saveToLog("Server started and listening on port 5000");   //Store first log message.
        
        
        //  Create the server socket and start accepting clients
        // try with resources ensures serverSocket is closed automatically if main exits.
        try(ServerSocket serverSocket = new ServerSocket(5000)){
            //server waits for a client.
             while (true) {    
                   // Accept() blocks until a new client connects. [1][2][6]
                Socket socket = serverSocket.accept();  
              
                // Log client connection event 
                ClientHandler.saveToLog("Client connected: "+ socket.getLocalAddress());   //Store message of client's entry log file. [6]
                // Create a new handler thread for this client.
                ClientHandler clientHandler = new ClientHandler(socket);
                
                // Add this client handler to the shared client list.
                ClientList.add(clientHandler); 
                
                
                // Start the handler thread, which will manage communication
                // with this specific client.
                clientHandler.start();     
            
             }//End of while loop.
        }catch(IOException e){
            System.out.println("Error in server: "+ e.getMessage());
        }//End of catch.
    }//End of main class.
 
 
        /*
     * Appends a new entry to the log file (LOG_FILE) with a timestamp.
     *
     * Log format:
     *   [yyyy-MM-dd HH:mm:ss] <message>
     
     */
 public static void saveToLog(String msg){  //To store messages in log file.
     // Create a timestamp based on current server local time. [5]
     String time= LocalDateTime.now().format(DATES_FORMAT);
        time= "["+time+"]"+" "+msg+" ";
        
        // FileWriter with append = true to append at end of file instead of overwriting. [1]
        try (PrintWriter writer = new PrintWriter(new FileWriter(ChatServer.LOG_FILE, true))) {
        writer.println(time);
        } catch (IOException e) {
            System.out.println("Error writing to log file: " + e.getMessage());
        }
    }//End saveToLog method.
 
    //-----------------------------------------------------------------------------------------------------------------
    /*
     * Broadcasts a message to ALL connected clients.
     *
     * This method iterates over ClientList and calls sendMessage(msg)
     * on every ClientHandler.
     *
     * The method is synchronized on ClientList to avoid concurrent modification
     * issues, since multiple client threads may trigger broadcast at the same time.
     *
     * @param msg The message string to send to all clients.
     */
 public static void broadcast(String msg){     //To broadcast message to ALL clients.
    synchronized (ClientList){  //"synchronized" helps with manageing threads. It closes access to ClientList until this process is finished. [2][3]  
        for (ClientHandler handler : ClientList) {
             handler.sendMessage(msg);
          }//End of for.
    }//End of synchronized.
 }//End of broadcast method.
 
    //-----------------------------------------------------------------------------------------------------------------
     /*
     * Counts how many lines currently exist in the log file.
     *return Number of lines in the log file.
     *Returns 0 if any I/O error occurs while reading the log.
     */
 public static int getLogCount(){
     int count=0;
     // BufferedReader is used to read the log file line by line. [7]
     try(BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))){
            // For each line in log file, increment counter.
         while (reader.readLine()!=null) 
             count++;
     }catch(IOException e){
         return 0;
     }
     return count;
 }//End of getLogCount method.
}
