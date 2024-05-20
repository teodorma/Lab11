package Bonus2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private Selector selector;
    private Map<String, List<SocketChannel>> topicSubscribers = new HashMap<>();
    private Map<SocketChannel, String> clientUsernames = new HashMap<>();

    public ChatServer() throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(PORT));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Chat server started on port " + PORT);
    }

    public void start() throws IOException {
        while (true) {
            selector.select();
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isAcceptable()) {
                    acceptClient(serverSocketKey(key));
                } else if (key.isReadable()) {
                    readClient(key);
                }
            }
        }
    }

    private ServerSocketChannel serverSocketKey(SelectionKey key) {
        return (ServerSocketChannel) key.channel();
    }

    private void acceptClient(ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("New client connected: " + client.getRemoteAddress());
        client.write(ByteBuffer.wrap("Enter your name:\n".getBytes()));
    }

    private void readClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            disconnectClient(client);
            return;
        }
        String message = new String(buffer.array()).trim();
        handleMessage(client, message);
    }

    private void disconnectClient(SocketChannel client) throws IOException {
        String username = clientUsernames.remove(client);
        if (username != null) {
            System.out.println(username + " disconnected.");
        }
        client.close();
    }

    private void handleMessage(SocketChannel client, String message) throws IOException {
        if (!clientUsernames.containsKey(client)) {
            String username = message;
            clientUsernames.put(client, username);
            client.write(ByteBuffer.wrap(("Welcome " + username + "! You can now subscribe to topics, send messages to topics, and send direct messages to other users.\n").getBytes()));
            System.out.println(username + " connected.");
        } else {
            String[] tokens = message.split(" ", 4);
            String command = tokens[0];
            if (command.equalsIgnoreCase("/subscribe") && tokens.length >= 2) {
                String topic = tokens[1];
                subscribeToTopic(client, topic);
            } else if (command.equalsIgnoreCase("/send") && tokens.length >= 3) {
                String recipientOrTopic = tokens[1];
                if (tokens[2].equalsIgnoreCase("@")) {
                    String topic = recipientOrTopic;
                    String msg = tokens[3];
                    broadcastMessage(client, topic, msg);
                } else {
                    String recipient = recipientOrTopic;
                    String msg = tokens[2];
                    sendMessageToUser(client, recipient, msg);
                }
            } else {
                client.write(ByteBuffer.wrap("Invalid command\n".getBytes()));
            }
        }
    }

    private void subscribeToTopic(SocketChannel client, String topic) throws IOException {
        topicSubscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(client);
        String username = clientUsernames.get(client);
        System.out.println(username + " subscribed to " + topic);
        client.write(ByteBuffer.wrap(("Subscribed to topic " + topic + "\n").getBytes()));
    }

    private void broadcastMessage(SocketChannel sender, String topic, String message) throws IOException {
        List<SocketChannel> subscribers = topicSubscribers.get(topic);
        if (subscribers != null) {
            String senderUsername = clientUsernames.get(sender);
            for (SocketChannel subscriber : subscribers) {
                if (subscriber != sender) {
                    subscriber.write(ByteBuffer.wrap(("[" + clientUsernames.get(subscriber) + "] " + senderUsername + " @ " + topic + ": " + message + "\n").getBytes()));
                }
            }
            System.out.println("Message from " + senderUsername + " to topic " + topic + ": " + message);
        } else {
            sender.write(ByteBuffer.wrap(("No subscribers to topic " + topic + "\n").getBytes()));
        }
    }

    private void sendMessageToUser(SocketChannel sender, String recipient, String message) throws IOException {
        for (Map.Entry<SocketChannel, String> entry : clientUsernames.entrySet()) {
            if (entry.getValue().equals(recipient)) {
                SocketChannel recipientChannel = entry.getKey();
                String senderUsername = clientUsernames.get(sender);
                recipientChannel.write(ByteBuffer.wrap(("[" + recipient + "] " + senderUsername + ": " + message + "\n").getBytes()));
                System.out.println("Message from " + senderUsername + " to " + recipient + ": " + message);
                return;
            }
        }
        sender.write(ByteBuffer.wrap(("User " + recipient + " not found\n").getBytes()));
    }

    public static void main(String[] args) {
        try {
            new ChatServer().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
