package ShapeShifters;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.Material;
import org.jogamp.vecmath.*;

public class BasicServer {
    private static final int PORT = 5001;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int nextPlayerId = 1;
    private static List<NPC> npcs = new ArrayList<>();
    private static ArrayList<ArrayList<Integer>> maze;
    private static int[][] movingWalls = new int[4][2];
    private static final int MAZE_HEIGHT = 20;
    private static final int MAZE_WIDTH = 20;

    public static void main(String[] args) {
        // Initialize maze and designate moving walls.
        maze = GenerateMaze.getMaze(MAZE_HEIGHT, MAZE_WIDTH);
        // Clear a central square in the maze.
        for (int i = 7; i < 13; i++) {
            for (int j = 7; j < 13; j++) {
                maze.get(i).set(j, 0);
            }
        }
        // Randomly remove 10% of walls.
        for (int i = 1; i < MAZE_HEIGHT - 1; i++) {
            for (int j = 1; j < MAZE_WIDTH - 1; j++) {
                if (Math.random() < 0.1) {
                    maze.get(i).set(j, 0);
                }
            }
        }
        // Designate moving walls.
        Random rand = new Random();
        int movingWallsIndex = 0;
        while (movingWallsIndex < 4) {
            int i = rand.nextInt(MAZE_HEIGHT - 2) + 1;
            int j = rand.nextInt(MAZE_WIDTH - 2) + 1;
            boolean found = false;
            if (maze.get(i).get(j) == 1) {
                for (int n = 0; n < movingWallsIndex; n++) {
                    if (movingWalls[n][0] == i && movingWalls[n][1] == j) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    movingWalls[movingWallsIndex] = new int[]{i, j};
                    movingWallsIndex++;
                }
            }
        }

        // Generate valid positions for NPCs from open maze cells.
        List<Vector3d> validPositions = new ArrayList<>();
        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (maze.get(i).get(j) == 0) {
                    double x = -1 + i * 0.103;
                    double z = -1 + j * 0.103;
                    validPositions.add(new Vector3d(x, 0.1, z));
                }
            }
        }

        // Create 3 NPCs with a green appearance.
        Appearance npcAppearance = new Appearance();
        npcAppearance.setMaterial(new Material(
                new Color3f(0.0f, 1.0f, 0.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(0.0f, 1.0f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));
        int npcCount = 3;
        for (int i = 0; i < npcCount; i++) {
            if (validPositions.isEmpty())
                break;
            NPC npc = NPC.generateRandomNPC(validPositions, npcAppearance, 0.01);
            npcs.add(npc);
        }

        // Update NPC positions and broadcast them periodically.
        new Thread(() -> {
            while (true) {
                for (NPC npc : npcs) {
                    npc.update((x, z) -> {
                        int gridX = (int) ((x + 1) / 0.103);
                        int gridZ = (int) ((z + 1) / 0.103);
                        return maze.get(gridX).get(gridZ) == 1;
                    });
                }
                broadcastNPCPositions();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

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

    public static synchronized void broadcastNPCPositions() {
        StringBuilder npcState = new StringBuilder("NPC_UPDATE");
        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            Vector3d pos = npc.getPosition();
            npcState.append(" ").append(i)
                    .append(" ").append(pos.x)
                    .append(" ").append(0.1)
                    .append(" ").append(pos.z);
        }
        for (ClientHandler client : clients) {
            client.sendMessage(npcState.toString());
        }
    }

    // Broadcast a message to all connected clients.
    public static synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
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
                // Send the player's ID.
                out.println("ID " + playerId);

                // Send maze data as a single string (each row concatenated).
                StringBuilder mazeStr = new StringBuilder();
                for (ArrayList<Integer> row : maze) {
                    for (Integer cell : row) {
                        mazeStr.append(cell);
                    }
                }
                out.println(mazeStr.toString());

                // Send moving wall coordinates.
                for (int[] coords : movingWalls) {
                    out.println(coords[0] + " " + coords[1]);
                }

                // Send NPC initialization info.
                out.println("NPC_COUNT " + npcs.size());
                for (NPC npc : npcs) {
                    Vector3d pos = npc.getPosition();
                    Vector3d dir = npc.getDirection();
                    out.println("NPC_INIT " + pos.x + " " + pos.z + " " + dir.x + " " + dir.z);
                }
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
