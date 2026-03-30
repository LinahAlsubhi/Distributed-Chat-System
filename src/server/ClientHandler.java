package Server;

/*
 * CPCS 371 FINAL PROJECT
 * GROUP NO. :5
 * SECTION: EAR
 *
 * ClientHandler.java
 *
 * This class represents the dedicated handler thread assigned to each connected
 * client. Every new client receives its own instance of ClientHandler, which:
 *
 *  - Reads user input from that specific client over TCP
 *  - Processes commands (/users, /log, /alert, /exit)
 *  - Broadcasts chat messages to other clients via ChatServer
 *  - Logs all important events (connection, disconnection, commands, messages)
 *  - Safely cleans up when a client exits
 *
 * Each ClientHandler runs independently on its own thread.
 *
 * References:
 * [1] Myles, A. (2001). Java TCP Sockets and Swing Tutorial.https://www.ashishmyles.com/tutorials/tcpchat/index.html
 * [2] Threads and Multiprocessing CH 12: https://math.hws.edu/javanotes/c12/index.html
 * [3] Schildt, H. ,JAVA: The Complete Reference, 9th ed CH 11: https://www.sietk.org/downloads/javabook.pdf
 * [4] RendoxThread, D., Runnable, Callable, ExecutorService, and Future - all the ways to create threads in Java: https://dev.to/danielrendox/thread-runnable-callable-executorservice-and-future-all-the-ways-to-create-threads-in-java-2o86
 * [5] Java Date and Time: https://www.w3schools.com/java/java_date.asp
 * [6] Class Socket: https://docs.oracle.com/javase/8/docs/api/java/net/Socket.html
 * [7] Different ways of Reading a text file in Java: https://www.geeksforgeeks.org/java/different-ways-reading-text-file-java/
 */

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

class ClientHandler extends Thread {    //[2]  Section 12.1

    // ----------------------- Data Fields -----------------------

    /** The TCP socket connecting this specific client to the server */
    private Socket socket;

    /** Buffered reader for receiving messages from the client */
    private BufferedReader in;

    /** Output stream for sending messages back to this client */
    PrintWriter out;

    /** Display name chosen by the client */
    private String clientName;

    /** Time format used for log file timestamps [5] */
    private static final DateTimeFormatter DATES_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Identifies client type.
     * Default = "Client". If name contains "ADMIN", they become "Admin".
     */
    private String clientType = "Client";


    // ----------------------- Constructor -----------------------

    /**
     * Creates a new handler for an incoming client connection.
     *
     * @param s The socket representing the client's TCP connection.
     */
    public ClientHandler(Socket s) {
        this.socket = s;
    }


    // ----------------------- Thread Execution -----------------------

    /**
     * The main execution method of the thread. 
     *
     * Responsibilities:
     *  - Initialize input/output streams
     *  - Request user name
     *  - Identify admin users
     *  - Listen for commands and messages
     *  - Log all activities
     *  - Handle clean disconnects or unexpected errors
     */
    @Override
    public void run() { //[2] Section 12.4

        try {
            // Set up I/O streams for communication. [1]
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Request client's display name. [1]
            out.println("Welcome to chat! Please enter your name: ");

            clientName = in.readLine();   // Blocking read for client's name
            ChatServer.broadcast(clientName.toUpperCase() + " has enterd the chat.");

            // Assign default name if blank
            if (clientName == null || clientName.isEmpty())
                clientName = "Anonymous";

            // Admin detection: If name begins with "ADMIN", assign role
            if (clientName.toUpperCase().contains("ADMIN")) {
                clientType = "Admin";
                // Remove the "ADMIN " prefix from displayed username
                clientName = clientName.substring(6, clientName.length());
            }

            // Log connection event
            saveToLog("Client connected: " + socket.getInetAddress()
                    + " (User: " + clientName + ")");

            // Notify all clients
            ChatServer.broadcast("[SERVER] " + clientName + " has enterd chat.");

            String msg;

            /**
             * MAIN LISTEN LOOP:
             * Continuously reads messages from this client.
             * readLine() returns null if client disconnects.
             */
            while ((msg = in.readLine()) != null) {

                // ----------------------- /users Command -----------------------
                if (msg.startsWith("/users")) {

                    String message = "Connected users: ";

                    // Build list of connected usernames
                    for (ClientHandler handler : ChatServer.ClientList) {
                        message += handler.clientName + ", ";
                    }

                    // Remove trailing comma & space before sending
                    out.println(message.substring(0, message.length() - 2));

                    saveToLog("Command /users requested by /" + socket.getInetAddress());
                }

                // ----------------------- /log Command -----------------------
                else if (msg.startsWith("/log")) {
                    int count = ChatServer.getLogCount();
                    out.println("Total messages logged: " + count);

                    saveToLog("Command /log requested by /" + socket.getInetAddress());
                }

                // ----------------------- /alert Command -----------------------
                else if (msg.startsWith("/alert")) {

                    // Verify admin permission
                    if (!clientType.contains("Admin")) {
                        out.println("[Server] NOT AN ADMIN!!");
                    } else {
                        // Extract alert message after "/alert"
                        String alertMsg = msg.substring(6).trim();

                        // Send to ALL clients
                        ChatServer.broadcast(alertMsg);

                        // Log the alert
                        saveToLog("Alert broadcasted: '" + alertMsg + "'");
                    }
                }

                // ----------------------- /exit Command -----------------------
                else if (msg.startsWith("/exit")) {
                    // Do nothing here. Cleanup happens in finally{} block.
                }

                // ----------------------- Normal Chat Message -----------------------
                else {
                    // Broadcast to all clients
                    ChatServer.broadcast(clientName + ": " + msg);

                    // Log the message
                    saveToLog("Message received from " + clientName + ": " + msg);
                }

            } // End while

        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }

        // ----------------------- Client Disconnect Cleanup -----------------------
        finally {   // [2]
            try {
                socket.close(); //[1]
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Remove client from active list
            ChatServer.ClientList.remove(this);

            // Broadcast departure to all clients
            ChatServer.broadcast("[SERVER] " + clientName + " has left the chat.");

            // Log disconnection
            saveToLog("Client disconnected: " + socket.getInetAddress()
                    + " (User: " + clientName + ")");
        }

    } // End run()


    // ----------------------- Helper Methods -----------------------

    /**
     * Writes a timestamped entry to the server's log file.
     *
     * Format:
     *   [yyyy-MM-dd HH:mm:ss] <message>
     *
     * @param msg The message to be logged.
     */
    static void saveToLog(String msg) {
        String time = LocalDateTime.now().format(DATES_FORMAT); //[5]
        String fMessage = "[" + time + "] " + msg + " ";

        try (PrintWriter writer = new PrintWriter(new FileWriter(ChatServer.LOG_FILE, true))) { //[1]
            writer.println(fMessage);
        } catch (IOException e) {
            System.out.println("Could not write to log file: " + e.getMessage());
        }
    }

    /**
     * Sends a message directly to this specific client.
     *
     * @param msg The message to send.
     */
    void sendMessage(String msg) {
        out.println(msg);
    }

} // End of ClientHandler class
