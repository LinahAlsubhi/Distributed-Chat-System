/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Client;
/**
 * CPCS 371 – Computer Networks 1
 * Final Project: Distributed Chat Logger System Using TCP Sockets
 * Group #: 5
 * Section: EAR
 *
 * ChatClient.java
 *
 * This class represents the client-side application in a distributed chat system.
 * It connects to the ChatServer using TCP sockets, sends user input, and receives
 * messages broadcasted by the server. Each running instance represents an active user
 * in the chat room.
 *
 * Main responsibilities:
 * - Establish TCP connection to server
 * - Send user text or commands (such as /users, /log, /alert)
 * - Receive and display incoming messages asynchronously
 * - Handle safe termination (/exit)
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

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient{
    //Default TCP port number where server is listening
    private static final int SERVER_PORT= 5000;
    //a scanner to read user keyboard input from console
    private static Scanner scanner = new Scanner(System.in);
    public static void main(String[] args) {
        try {
            //Ask user for server IP address dynamically 
            System.out.print("Enter Server IP Address: ");
            String serverAddress = scanner.nextLine();
            
            
            /*
            create a tcp socket connection to server
             * - serverAddress: IPv4 or hostname 
             * - SERVER_PORT: must match server’s listening port
             *
             * If connection succeeds: a TCP handshake occurs
             */

            Socket socket = new Socket(serverAddress, SERVER_PORT);    //Create a client socket. [1][2][3]
            System.out.println("connected to server!");
            
             /*
             * start background listener thread 
             * Purpose: Receive messages from server continuously while client
             * is still able to type messages at same time.
             *
             * Each TCP client must listen to server on a specific thread,
             * otherwise input blocking prevents receiving broadcasts.
             */
            Thread listenerThread = new Thread(new Runnable() { //Create thread to read messages from server seperatly. [2][4]
                @Override
                public void run() {
                    try {
                        /*
                         * BufferedReader reads text lines sent from server.
                         */
                        
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));   //To read from server. [1]
                String serverMsg;
               
                while ((serverMsg=in.readLine())!=null)    //As long as there is messages from server, print that message.
                    System.out.println(serverMsg);
                
        
            } catch (IOException e) {
                       /**
                         * Happens when:
                         * - server shuts down
                         * - network failure
                         * - forceful disconnect
                         */
                System.out.println("Server conncetion closed or Error reading message.");
                System.exit(0);     //Close program if there is problem at connecting with server.
            } 
                    }
            }); //End of thread block.
            
            listenerThread.start();     //Start the thread to start listening to server. [2][3][4]
            
            /*
             * PrintWriter sends text messages to server.
             * autoFlush=true then it flushes output after every println(),
             * ensuring messages are delivered immediately without buffering delay.
             */
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);  //To write to server. [1]
            
            
            /*
             * Main input loop:
             * Continuously read user input and send it to server.
             *
             * Supports:
             * - Regular chat messages
             * - Commands:
             *      /users: request active clients list
             *      /log:  request total logged messages
             *      /alert <msg>:  admin broadcast
             *      /exit: disconnect
             */
            while (true) {  //Continued loop to recieve from server.
                String clientMsg= scanner.nextLine();
                // Send typed message to server over TCP connection
                out.println(clientMsg); 
                if (clientMsg.equals("/exit"))    //If client want to exit then stop program. 
                    break;
                
                //Close streams.
                
            }//End of while
            
            //Close socket and streams. 
            socket.close(); //[1]
            scanner.close();
            
        } catch (IOException e) {
                       /**
             * Triggered if:
             * - server not running
             * - wrong IP or unreachable network
             */
            System.out.println("Error: client could not connect to server"+ e.getMessage());
        }//End of catch.
    }
}
