package cs494p.irc;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server implements Runnable {
    static class ConnectionHandler implements Runnable{
        Socket socket;
        DataInputStream input;
        DataOutputStream output;
        String nickname = "";
        boolean isActive = false;
    
        public ConnectionHandler(Socket socket) {
            try {
                isActive = true;
                this.socket = socket;
                //System.out.println("Fetching client input stream...");
                this.input = new DataInputStream(socket.getInputStream());
                //System.out.println("Fetching client output stream...");
                this.output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                System.err.println("Failed to create ConnectionHandler:\n" + ex);
            }
        }

        private void greetClient() {
            System.out.println("Greeting client...");
            String clientMessage = "";
            try {
                output.writeUTF("Hello! Please enter a nickname: ");
                boolean isCleared = false;
                while (isCleared == false) {
                    clientMessage = input.readUTF();
                    nickname = clientMessage.split("\s")[0];
                    output.writeUTF("Nickname: [" + nickname + "] Is this correct? (y/n)");
                    clientMessage = input.readUTF();
                    if (clientMessage.equals("y")) {
                        output.writeUTF("OK - nickname " + nickname + " accepted!");
                        isCleared = true;
                        System.out.println("Client " + nickname + " connected.");
                    } else {
                        output.writeUTF("Please enter a nickname: ");
                    }
                }
            } catch (Exception ex) {
                disconnect();
            }
        }
    
        public void disconnect() {
            System.out.println("Disconnecting client: " + nickname);
            isActive = false;
            try {
                input.close();
                output.close();
                socket.close();
            } catch (IOException ex) {
                System.err.println("Failed to disconnect client:\n" + ex);
            }
            System.out.println("Client disconnected.");
        }

        public void run() {
            greetClient();
            String message = "";
            try {
                while (isActive == true && socket.isConnected()) {
                    message = input.readUTF();
                    System.out.println(nickname + ": " + message);
                }
            } catch (IOException ex) {
                System.err.println("");
            }
        }
    }

    static class MessageSender implements Runnable {
        DataOutputStream dos;
        String message;

        public MessageSender(DataOutputStream dos, String message) {
            this.dos = dos;
            this.message = message;
        }

        public void run() {
            try {
                dos.writeUTF(message);
            } catch (Exception ex) {
                System.err.println("Failed to send message: " + message);
            }
        }
    }

    Vector<ConnectionHandler> activeConnections = null;
    ServerSocket serverSocket = null;
    BufferedReader br = null;
    boolean isOnline = true;

    public Server(int port) {
        try {
            System.out.println("Starting server...");
            serverSocket = new ServerSocket(port);
            activeConnections = new Vector<>();
            br = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException ex) {
            System.err.println("Failed to start server:\n" + ex);
        }
    }

    public void run() {
        Thread reaper = new Thread(new Runnable() {
            public void run() {
                while (isOnline) {
                    try {
                        Thread.sleep(10000);
                        System.out.println("Reaping...");

                        Iterator<ConnectionHandler> i = activeConnections.iterator();
                        while (i.hasNext()) {
                            ConnectionHandler connection = i.next();
                            System.out.println("reaper: checking client " + connection.nickname);
                            try {
                                connection.output.writeUTF("PING");
                                connection.output.flush();
                            } catch (Exception ex) {
                                System.out.println("No response from client " + connection.nickname);
                                connection.isActive = false;
                            }
                            if (connection.isActive == false) {
                                connection.disconnect();
                                i.remove();
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("reaper: " + ex);
                    }
                    System.out.println("Reaping complete.");
                }
            }
        });

        Thread console = new Thread(new Runnable() {
            public void run() {
                String serverCommand = "";
                while (serverSocket.isBound()) {
                    try {
                        serverCommand = br.readLine();
                        if (serverCommand.equals("ONLINE")) {
                            for (ConnectionHandler connection : activeConnections) {
                                System.out.println(connection.nickname + " : " + (connection.isActive ? "online":"offline"));
                            }
                        }
                        if (serverCommand.equals("QUIT")) {
                            isOnline = false;
                            Iterator<ConnectionHandler> i = activeConnections.iterator();
                            while (i.hasNext()) {
                                ConnectionHandler connection = i.next();
                                connection.output.writeUTF("Server is shutting down. Goodbye.");
                                connection.disconnect();
                                i.remove();
                            }
                        }
                    } catch (IOException ex) {
                        System.err.println("console: " + ex);
                    }
                }
            }
        });

        reaper.start();
        console.start();

        System.out.println("Server running.");
        while (isOnline) {
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

    public static void main(String[] args) {
        int port = 6667;
        Server server = new Server(port);
        Thread thread = new Thread(server);
        thread.start();
    }
}