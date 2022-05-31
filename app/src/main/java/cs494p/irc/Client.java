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
            System.out.println("Connecting to server @ " + address + ":" + port);
            this.socket = new Socket(address, port);
            System.out.println("Fetching input stream...");
            this.input = new DataInputStream(socket.getInputStream());
            System.out.println("Fetching output stream...");
            this.output = new DataOutputStream(socket.getOutputStream());
            this.br = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException ex) {
            System.err.println("Failed to create Client:\n" + ex);
        }
    }

    private void greetServer() {        
        System.out.println("Greeting server...");
        
        String serverMessage = "";
        String clientMessage = "";

        try {
            serverMessage = input.readUTF();
            System.out.println(serverMessage);  // Server greeting, ask for nickname
            clientMessage = br.readLine();
            output.writeUTF(clientMessage);     // Client submit nickname
            output.flush();
            serverMessage = input.readUTF();    // Server asks for confirmation
            System.out.println(serverMessage);
            clientMessage = br.readLine();      // Client responds y/n
            output.writeUTF(clientMessage);
            output.flush();
            serverMessage = input.readUTF();    // Server gives OK or asks for new nickname
            System.out.println(serverMessage);

            while (!serverMessage.startsWith("OK")) {
                clientMessage = br.readLine();  // Client responds with new nickname
                output.writeUTF(clientMessage);
                output.flush();
                serverMessage = input.readUTF();    // Server asks for confirmation
                System.out.println(serverMessage);
                clientMessage = br.readLine();      // Client responds y/n
                output.writeUTF(clientMessage);
                output.flush();
                serverMessage = input.readUTF();    // Server gives OK or asks for new nickname
                System.out.println(serverMessage);
            }
        } catch (Exception ex) {
            System.err.println("Connection to server has been lost.");
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
                while (isActive && socket.isConnected()) {
                    try {
                        String message = br.readLine();
                        output.writeUTF(message);
                        output.flush();
                    } catch (Exception ex) {
                        System.err.println("Connection to server has been lost.");
                        disconnect();
                    }
                }
            }
        });

        Thread receiveMessage = new Thread(new Runnable() {
            public void run() {
                while (isActive && socket.isConnected()) {
                    try {
                        String message = input.readUTF();
                        if (!message.equals("PING")) {
                            System.out.println(message);
                        }
                    } catch (Exception ex) {
                        System.err.println("Connection to server has been lost.");
                        disconnect();
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
