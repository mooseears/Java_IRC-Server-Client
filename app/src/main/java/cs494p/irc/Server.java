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
        private HashSet<Channel> channels;
        private Channel activeChannel;
        private Channel defaultChannel;
        boolean isActive = false;
    
        public ConnectionHandler(Socket socket, String nick) {
            try {
                isActive = true;
                nickname = nick;
                channels = new HashSet<>();
                defaultChannel = Server.generalChannel;
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
                    sendNotification(connectMessage);
                    System.out.println(connectMessage);
                    joinChannel(generalChannel);
                } else {
                    System.out.println("Rejected client.");
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
                this.leaveChannel(channel);
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

        public void sendNotification(String message) {
            try {
                output.writeUTF(message);
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }

        public void joinChannel(Channel channel) {
            if (!channels.contains(channel)) {
                channels.add(channel);
                channel.addUser(this);
                activeChannel = channel;
            } else {
                sendNotification("<server>: You are already a member of channel [" + channel.name + "]");
            }
        }

        public void leaveChannel(Channel channel) {
            if (channels.contains(channel)) {
                channel.removeUser(this);
                channels.remove(channel);
                if (activeChannel == channel) {
                    activeChannel = defaultChannel;
                }
                sendNotification("<server>: You have left channel [" + channel.name + "]");
            } else {
                sendNotification("<server>: You are not a member of channel [" + channel.name + "]");
            }
        }

        public String listChannels() {
            StringBuilder sb = new StringBuilder();
            sb.append("--- Joined Channels ---\n");
            synchronized (openChannels) {
                for (Channel channel : channels) {
                    sb.append(channel.name + " : " + channel.topic + "\n");
                }
            }
            return sb.toString();
        }

        public void changeNickname(String nick) {
            boolean exists = false;
            synchronized (Server.activeConnections) {
                for (ConnectionHandler user : Server.activeConnections) {
                    if (user.nickname.equals(nick)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    sendNotification("<server>: Nickname " + nick + " already in use.");
                } else {
                    String temp = nickname;
                    nickname = nick;
                    activeChannel.broadcast("<channel>", temp + " has changed their name to " + nick);
                }
            }
        }

        public void run() {
            greetClient();
            String message = "";
            try {
                while (isActive == true && socket.isConnected()) {
                    message = input.readUTF();
                    MessageParser parser = new MessageParser(this, message);
                    Thread thread = new Thread(parser);
                    thread.start();
                }
            } catch (IOException ex) {
                System.err.println("");
            }
        }
    }

    static class MessageParser implements Runnable {
        ConnectionHandler user;
        String message;

        public MessageParser(ConnectionHandler user, String message) {
            this.user = user;
            this.message = message;
        }

        public void run() {
            String[] splitMessage = message.split("\s");
            String command = splitMessage[0].toUpperCase();
            if (command.startsWith("/")) {
                if (splitMessage.length == 1) {
                    switch (command) {
                        case "/HELP":
                            break;
                        case "/QUIT":
                            user.disconnect();
                            break;
                        case "/TOPIC":
                            user.sendNotification(user.activeChannel.getTopic());
                            break;
                        case "/MYCHANNELS":
                            String myChannels = user.listChannels();
                            user.sendNotification(myChannels);
                            break;
                        case "/CHANNELS":
                            String channelList = Server.listChannels();
                            user.sendNotification(channelList);
                            break;
                        case "/MEMBERS":
                            String memberList = user.activeChannel.listMembers();
                            user.sendNotification(memberList);
                            break;
                    }
                } else {
                    String arg = splitMessage[1];
                    switch (command) {
                        case "/NICK":
                            user.changeNickname(splitMessage[1]);
                            break;
                        case "/JOIN":
                            if (!arg.startsWith("#")) {
                                arg = "#" + arg;
                            }
                            boolean joined = false;
                            synchronized (Server.openChannels) {
                                for (Channel c : Server.openChannels) {
                                    if (c.name.equals(arg)) {
                                        user.joinChannel(c);
                                        joined = true;
                                        break;
                                    }
                                }
                                if (joined == false) {
                                    user.joinChannel(Server.createChannel(arg));
                                }
                            }
                            break;
                        case "/LEAVE":
                            boolean left = false;
                            if (!arg.startsWith("#")) {
                                arg = "#" + arg;
                            }
                            for (Channel c : user.channels) {
                                if (c.name.equals(arg)) {
                                    user.leaveChannel(c);
                                    left = true;
                                    break;
                                }
                            }
                            if (left == false) {
                                user.sendNotification("<server>: You are not a member of channel " + arg);
                            }
                            break;
                        case "/SWITCH":
                            boolean switched = false;
                            if (!arg.startsWith("#")) {
                                arg = "#" + arg;
                            }
                            for (Channel c : user.channels) {
                                if (c.name.equals(arg)) {
                                    user.activeChannel = c;
                                    switched = true;
                                    break;
                                }
                            }
                            if (switched == false) {
                                user.sendNotification("<server>: You are not a member of channel " + arg);
                            }
                            break;
                        case "/TOPIC":
                            String topic = message.substring(command.length() + 1);
                            user.activeChannel.changeTopic(topic);
                            break;
                        default:
                            user.sendNotification("<server>: Invalid command: " + message);
                    }
                } 
            } else {
                user.activeChannel.broadcast(user.nickname, message);
            }
        }
    }

    static class Channel implements Runnable {
        Vector<ConnectionHandler> members;
        String name;
        String topic;
        boolean isOpen = true;
        
        public Channel(String name) {
            members = new Vector<>();
            this.name = name;
            this.topic = "No topic";
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
                user.leaveChannel(this);
                members.remove(user);
            }
        }

        public void addUser(ConnectionHandler user) {
            members.add(user);
            broadcast("<channel>", user.nickname + " has joined.");
        }

        public void removeUser(ConnectionHandler user) {
            members.remove(user);
            broadcast("<channel>", user.nickname + " has left.");
        }

        public String listMembers() {
            StringBuilder sb = new StringBuilder();
            sb.append("--- [" + this.name + "] Members ---\n");
            synchronized (members) {
                for (ConnectionHandler member : members) {
                    sb.append("\t" + member.nickname + "\n");
                }
            }
            return sb.toString();
        }

        public String getTopic() {
            return "[" + name + "] TOPIC: " + topic;
        }

        public void changeTopic(String newTopic) {
            this.topic = newTopic;
        }

        public void broadcast(String sender, String message) {
            String fullMessage = "[" + this.name + "] " + sender + ": " + message;
            for (ConnectionHandler user : members) {
                try {
                    user.sendNotification(fullMessage);
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            }
        }

        public void run() {
            while (isOpen) {

            }
        }
    }   

    static Vector<ConnectionHandler> activeConnections = null;
    static Set<Channel> openChannels = null;
    static Channel generalChannel = null;
    ServerSocket serverSocket = null;
    BufferedReader br = null;
    static boolean isOnline = true;
    int clientNum = 0;

    public Server(int port) {
        try {
            System.out.println("Starting server...");
            serverSocket = new ServerSocket(port);
            activeConnections = new Vector<>();
            openChannels = new HashSet<>();
            generalChannel = createChannel("#general");
            br = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException ex) {
            System.err.println("Failed to start server:\n" + ex);
        }
    }

    static public Channel createChannel(String name) {
        Channel channel = new Channel(name);
        openChannels.add(channel);
        return channel;
    }

    static public String listChannels() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Server Channels ---\n");
        synchronized (openChannels) {
            for (Channel channel : openChannels) {
                sb.append(channel.name + " : " + channel.topic + "\n");
            }
        }
        return sb.toString();
    }

    String listMembers() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Server Members ---\n");
        synchronized (activeConnections) {
            for (ConnectionHandler member : activeConnections) {
                sb.append(member.nickname + "\n");
            }
        }
        return sb.toString();
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
                        if (serverCommand.equals("/MEMBERS")) {
                            System.out.println(listMembers());
                        }
                        if (serverCommand.equals("/QUIT")) {
                            Iterator<ConnectionHandler> i = activeConnections.iterator();
                            while (i.hasNext()) {
                                ConnectionHandler connection = i.next();
                                connection.sendNotification("<Server>: Server is shutting down. Goodbye.");
                                connection.disconnect();
                                i.remove();
                            }
                            isOnline = false;
                        }
                        if (serverCommand.equals("/CHANNELS")) {
                            System.out.println(listChannels());
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

        while (Server.isOnline) {
        }
        
        System.exit(0);
    }
}