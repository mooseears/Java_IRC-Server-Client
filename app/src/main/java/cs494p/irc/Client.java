package cs494p.irc;

import java.net.*;
import java.io.*;

public class Client implements Runnable {
    Socket socket;
    DataInputStream input;
    DataOutputStream output;
    BufferedReader br;
    public boolean isActive;

    public Client(String address, int port) {
        isActive = true;
        try {
            System.out.println("Connecting to server " + address + ":" + port);
            this.socket = new Socket(address, port);
            this.input = new DataInputStream(socket.getInputStream());
            this.output = new DataOutputStream(socket.getOutputStream());
            this.br = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException ex) {
            System.err.println("Failed to create Client:\n" + ex);
        }
    }

    private void greetServer() {        
        System.out.println("Greeting server...");
        try {
            output.writeUTF("Hello, server!");
            String serverMessage = input.readUTF();
            System.out.println(serverMessage);
        } catch (Exception ex) {
            System.err.println("Rejected by server.");
            disconnect();
        }
    }

    public void disconnect() {
        isActive = false;
        try {
            this.input.close();
            this.output.close();
            this.socket.close();
        } catch (IOException ex) {
            System.err.println("Failed to disconnect from server:\n" + ex);
        }
        System.out.println("Connection closed.");
    }

    public void run() {
        greetServer();

        Thread sendMessage = new Thread(new Runnable() {
            public void run() {
                while (isActive) {
                    try {
                        String message = br.readLine();
                        output.writeUTF(message);
                        output.flush();
                    } catch (Exception ex) {
                        if (isActive) {
                            System.err.println("Connection to server has been lost.");
                            disconnect();
                        }
                    }
                }
            }
        });

        Thread receiveMessage = new Thread(new Runnable() {
            public void run() {
                String message = "";
                while (isActive) {
                    try {
                        while ((message = input.readUTF()) != null) {
                            if (!message.equals("PING")) {
                                System.out.println(message);
                            }
                        }
                    } catch (Exception ex) {
                        if (isActive) {
                            System.err.println("Connection to server has been lost.");
                            disconnect();
                        }
                    }
                }
            }
        });

        sendMessage.start();
        receiveMessage.start();

        while (isActive) {
            try {
                sendMessage.join();
                receiveMessage.join();
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }
    }
    
    public static void main(String[] args) {
        String address = "127.0.0.1";
        int port = 6667;

        Client client = new Client(address, port);
        Thread thread = new Thread(client);
        thread.start();
        try { 
            thread.join();
        } catch (Exception ex) {
            System.err.println(ex);
            System.exit(1);
        }
        System.exit(0);
    }
}