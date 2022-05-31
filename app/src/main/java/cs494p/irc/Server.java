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
        boolean isCleared = false;
    
        public ConnectionHandler(Socket socket, String nick) {
            try {
                isActive = true;
                nickname = nick;
                this.socket = socket;
                this.input = new DataInputStream(socket.getInputStream());
                this.output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                System.err.println("Failed to create ConnectionHandler:\n" + ex);
            }
        }

        private void greetClient() {
            System.out.println("Greeting client...");
            try {
                String clientMessage = input.readUTF();
                if (clientMessage.equals("Hello, server!")) {
                    String connectMessage = (nickname + " connected to server.");
                    output.writeUTF(connectMessage);
                    System.out.println(connectMessage);
                } else {
                    System.out.println("Rejected rude client.");
                    disconnect();
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
    int clientNum = 0;

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

                        Iterator<ConnectionHandler> i = activeConnections.iterator();
                        while (i.hasNext()) {
                            ConnectionHandler connection = i.next();
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
                }
            }
        });

        Thread console = new Thread(new Runnable() {
            public void run() {
                String serverCommand = "";
                while (isOnline) {
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
                ConnectionHandler handler = new ConnectionHandler(clientSocket, "guest" + clientNum);
                clientNum++;
                Thread thread = new Thread(handler);
                thread.start();
                activeConnections.add(handler);
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