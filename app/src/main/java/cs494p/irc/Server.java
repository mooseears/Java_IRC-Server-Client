package cs494p.irc;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
    static class ConnectionHandler implements Runnable{
        Socket socket;
        DataInputStream input;
        DataOutputStream output;
        String nickname;
        boolean isActive = false;
    
        public ConnectionHandler(Socket socket) {
            try {
                this.socket = socket;
                System.out.println("Fetching client input stream...");
                this.input = new DataInputStream(socket.getInputStream());
                System.out.println("Fetching client output stream...");
                this.output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                System.err.println("Failed to create ConnectionHandler:\n" + ex);
            }
        }
    
        public void run() {
            isActive = true;
            System.out.println("Client " + socket + " connected.");
            String message = "";
            try {
                while (isActive == true) {
                    message = input.readUTF();
                    System.out.println("Client " + socket + ": " + message);
                }
            } catch (Exception ex) {
    
            }
        }
    }

    private static int port = 6667;

    public static void main(String[] args) {
        Vector<ConnectionHandler> activeConnections = new Vector<>();
        ServerSocket serverSocket = null;

        try {
            System.out.println("Starting server...");
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.err.println("Failed to start server:\n" + ex);
        }

        System.out.println("Server running.");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client request received: " + clientSocket);
                System.out.println("Creating new handler for client: " + clientSocket);
                ConnectionHandler handler = new ConnectionHandler(clientSocket);
                System.out.println("Adding handler to active connections...");
                activeConnections.add(handler);
                Thread thread = new Thread(handler);
                thread.start();

            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }
}
