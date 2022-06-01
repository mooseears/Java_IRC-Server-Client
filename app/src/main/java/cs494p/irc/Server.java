package cs494p.irc;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server implements Runnable {
    static class ConnectionHandler implements Runnable{
        private Socket socket;
        private DataInputStream input;
        private DataOutputStream output;
        public String nickname = "";
        private Vector<Channel> channels;
        private Channel activeChannel;
        private Channel defaultChannel;
        boolean isActive = false;
    
        public ConnectionHandler(Socket socket, String nick) {
            try {
                isActive = true;
                nickname = nick;
                    channels = new Vector<>();
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
                    sendMessage(connectMessage);
                    System.out.println(connectMessage);
                    for (Channel c : Server.openChannels) {
                        System.out.println("honk");
                        if (c.name == "#general") {
                            channels.add(c);
                            c.addUser(this);
                            break;
                        }
                    }
                } else {
                    System.out.println("Rejected rude client.");
                    disconnect();
                }
            } catch (Exception ex) {
                System.err.println(ex);
                disconnect();
            }
        }
    
        public void disconnect() {
            System.out.println("Disconnecting client: " + nickname);
            isActive = false;
            Iterator<Channel> it = channels.iterator();
            while (it.hasNext()) {
                Channel channel = it.next();
                channel.removeUser(this);
                this.removeFromChannel(channel);
            }
            
            try {
                input.close();
                output.close();
                socket.close();
            } catch (IOException ex) {
                System.err.println("Failed to disconnect client:\n" + ex);
            }
            System.out.println("Client disconnected.");
        }

        public void sendMessage(String message) {
            try {
                output.writeUTF(message);
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }

        public void addToChannel(Channel channel) {
            channels.add(channel);
            activeChannel = channel;
        }

        public void removeFromChannel(Channel channel) {
            channels.remove(channel);
            if (activeChannel == channel) {
                activeChannel = defaultChannel;
            }
        }

        public void run() {
            greetClient();
            String message = "";
            try {
                while (isActive == true && socket.isConnected()) {
                    message = input.readUTF();
                    activeChannel.broadcast(this.nickname, message);
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

    static class Channel implements Runnable {
        String name;
        String topic;
        Vector<ConnectionHandler> members;
        boolean isOpen = true;
        
        public Channel(String name) {
            members = new Vector<>();
            this.name = name;
            this.topic = "";
        }

        public Channel(String name, String topic) {
            members = new Vector<>();
            this.name = name;
            this.topic = topic;
        }

        public void closeChannel() {
            isOpen = false;
            broadcast("<channel>", "Channel is closing.");
            for (ConnectionHandler user : members) {
                user.removeFromChannel(this);
                members.remove(user);
            }
        }

        public void addUser(ConnectionHandler user) {
            members.add(user);
            user.activeChannel = this;
            broadcast("<channel>", user.nickname + " has joined.");
        }

        public void removeUser(ConnectionHandler user) {
            members.remove(user);
            broadcast("<channel>", user.nickname + " has left.");
        }

        public String listMembers() {

            return "Not implemented yet";
        }

        public void broadcast(String sender, String message) {
            String fullMessage = "[" + this.name + "] " + sender + ": " + message;
            for (ConnectionHandler user : members) {
                //if (user.nickname != sender) {
                    try {
                        user.sendMessage(fullMessage);
                    } catch (Exception ex) {
                        System.err.println(ex);
                    }
                //}
            }
        }

        public void run() {
            while (isOpen) {

            }
        }
    }   

    static class Commander implements Runnable {
        String message;
        ConnectionHandler user;

        public Commander(String message) {
            this.message = message;
        }

        public Commander(String message, ConnectionHandler user) {
            this.message = message;
            this.user = user;
        }



        public void run() {

        }
    }

    static class ServerManager implements Runnable {

        public void run() {

        }
    }

    static Vector<ConnectionHandler> activeConnections = null;
    static Set<Channel> openChannels = null;
    static ServerManager serverManager = null;
    ServerSocket serverSocket = null;
    BufferedReader br = null;
    boolean isOnline = true;
    int clientNum = 0;

    public Server(int port) {
        try {
            System.out.println("Starting server...");
            serverSocket = new ServerSocket(port);
            activeConnections = new Vector<>();
            openChannels = new HashSet<>();
            openChannels.add(new Channel("#general", "General chat"));
            br = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException ex) {
            System.err.println("Failed to start server:\n" + ex);
        }
    }

    public void run() {
        // reaper process checks for disconnected clients and boots them from the server.
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
                            } catch (Exception ex) {
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

        // console process allows for commands to be entered on the server side
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
                            Iterator<ConnectionHandler> i = activeConnections.iterator();
                            while (i.hasNext()) {
                                ConnectionHandler connection = i.next();
                                connection.sendMessage("<Server>: Server is shutting down. Goodbye.");
                                connection.disconnect();
                                i.remove();
                            }
                            isOnline = false;
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

        while (server.isOnline) {
        }
        
        System.exit(0);
    }

    public static enum Commands {
        NICK() {

        },
        MSG() {

        },
        JOIN() {

        },
        LEAVE() {

        },
        SWITCH() {

        },
        LIST() {

        },
        MEMBERS() {

        },
        QUIT() {

        },
    };
}