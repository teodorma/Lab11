package Task3Bonus1;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Map<String, List<ClientHandler>> topicSubscribers = new HashMap<>();
    private static Map<String, ClientHandler> userHandlers = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static synchronized void subscribeToTopic(String topic, ClientHandler clientHandler) {
        topicSubscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(clientHandler);
        System.out.println(clientHandler.getUserName() + " subscribed to " + topic);
    }

    static synchronized void broadcastMessage(String topic, String sender, String message) {
        List<ClientHandler> subscribers = topicSubscribers.get(topic);
        if (subscribers != null) {
            for (ClientHandler clientHandler : subscribers) {
                if (!clientHandler.getUserName().equals(sender)) {
                    clientHandler.sendMessage("[" + clientHandler.getUserName() + "] " + sender + " @ " + topic + ": " + message);
                }
            }
            System.out.println("Message from " + sender + " to topic " + topic + ": " + message);
        }
    }

    static synchronized void sendMessageToUser(String recipient, String sender, String message) {
        ClientHandler clientHandler = userHandlers.get(recipient);
        if (clientHandler != null) {
            clientHandler.sendMessage("[" + recipient + "] " + sender + ": " + message);
            System.out.println("Message from " + sender + " to " + recipient + ": " + message);
        } else {
            System.out.println("User " + recipient + " not found.");
        }
    }

    static synchronized void addUserHandler(String userName, ClientHandler clientHandler) {
        userHandlers.put(userName, clientHandler);
    }

    static synchronized void removeUserHandler(String userName) {
        userHandlers.remove(userName);
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String userName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUserName() {
        return userName;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            out.println("Enter your name:");
            userName = in.readLine();
            ChatServer.addUserHandler(userName, this);
            out.println("Welcome " + userName + "! You can now subscribe to topics, send messages to topics, and send direct messages to other users.");
            System.out.println(userName + " connected.");

            String input;
            while ((input = in.readLine()) != null) {
                String[] tokens = input.split(" ", 4);
                if (tokens[0].equalsIgnoreCase("/subscribe")) {
                    ChatServer.subscribeToTopic(tokens[1], this);
                } else if (tokens[0].equalsIgnoreCase("/send")) {
                    if (tokens.length == 4 && tokens[2].equalsIgnoreCase("@")) {
                        // Send to topic
                        ChatServer.broadcastMessage(tokens[1], userName, tokens[3]);
                    } else if (tokens.length == 3) {
                        // Send direct message
                        ChatServer.sendMessageToUser(tokens[1], userName, tokens[2]);
                    } else {
                        out.println("Invalid command format");
                    }
                } else {
                    out.println("Unknown command");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                ChatServer.removeUserHandler(userName);
                System.out.println(userName + " disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
