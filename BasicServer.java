package ShapeShifters;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class BasicServer {
    private static final int PORT = 5001;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int nextPlayerId = 1;
    private static ArrayList<ArrayList<Integer>> maze;
    private static final int MAZE_HEIGHT = 20;
    private static final int MAZE_WIDTH = 20;
    public static void main(String[] args) {
        maze = GenerateMaze.getMaze(20, 20);
        for (int i = 1; i < MAZE_HEIGHT-1; i++) {
            for (int j = 1; j < MAZE_WIDTH-1; j++) {
                if (Math.random() < .1) { //randomly remove 10% of the walls
                    maze.get(i).set(j, 0);
                }
            }
        }

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
            // Send update to all clients
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
                StringBuilder mazeStr = new StringBuilder();
                for (ArrayList<Integer> row : maze) {
                    for (Integer cell : row) {
                        mazeStr.append(cell);
                    }
                }
                out.println(mazeStr.toString());
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