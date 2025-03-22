package ShapeShifters;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class BasicServer {
    private static final int PORT = 5001;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int nextPlayerId = 1;

    public static void main(String[] args) {
        System.out.println("Server starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                ClientHandler handler = new ClientHandler(clientSocket, nextPlayerId++);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcasts a message to all connected clients.
    public static synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            // Send update to all clients.
            client.sendMessage(message);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int playerId;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Send the player's ID to the client (e.g., "ID 1")
                out.println("ID " + playerId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from player " + playerId + ": " + inputLine);
                    // Broadcast any update from one client to all clients.
                    broadcast(inputLine, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) { }
            }
        }
    }
}