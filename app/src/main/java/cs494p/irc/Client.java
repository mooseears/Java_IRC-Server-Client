package cs494p.irc;

import java.net.*;
import java.io.*;

public class Client {
    private static String address = "127.0.0.1";
    private static int port = 6667;
    
    public static void main(String[] args) {
        try {
            Socket socket = new Socket(address, port);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            Thread sendMessage = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            String message = br.readLine();
                            output.writeUTF(message);
                        } catch (IOException ex) {
                            System.err.println(ex);
                        }
                    }
                }
            });

            Thread receiveMessage = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            String message = input.readUTF();
                            System.out.println(message);
                        } catch (IOException ex) {
                            System.err.println(ex);
                        }
                    }
                }
            });

            sendMessage.start();
            receiveMessage.start();

        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
